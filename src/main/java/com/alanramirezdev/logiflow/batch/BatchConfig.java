package com.alanramirezdev.logiflow.batch;

import com.alanramirezdev.logiflow.dto.TelemetryCsvRecord;
import com.alanramirezdev.logiflow.entity.LogiflowTelemetry;
import com.alanramirezdev.logiflow.repository.LogiflowTelemetryRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.Executors;

@Configuration
public class BatchConfig {

    /**
     * Cada ejecución del Chunk se procesa de manera concurrente sin agotar el pool de hilos físicos del servidor.
     */
    @Bean
    public TaskExecutor taskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Se usa @StepScope para permitir la resolución dinámica de parámetros en tiempo de ejecución.
     */
    @Bean
    @StepScope
    public FlatFileItemReader<TelemetryCsvRecord> reader(@Value("#{jobParameters['filePath']}") String filePath) {
        return new FlatFileItemReaderBuilder<TelemetryCsvRecord>()
                .name("telemetryCsvReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)
                .delimited()
                .names("tripId", "vehicleVin", "driverId", "timestampUtc", "odometerKm", "fuelConsumedL", "vehicleStatus", "routeCode")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(TelemetryCsvRecord.class);
                }})
                .saveState(false)
                .build();
    }

    /**
     * Procesamiento por lotes.
     */
    @Bean
    public RepositoryItemWriter<LogiflowTelemetry> writer(LogiflowTelemetryRepository repository) {
        return new RepositoryItemWriterBuilder<LogiflowTelemetry>()
                .repository(repository)
                .methodName("save")
                .build();
    }

    /**
     * Se inyecta el taskExecutor basado en hilos virtuales
     */
    @Bean
    public Step importTelemetryStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    FlatFileItemReader<TelemetryCsvRecord> reader,
                                    TelemetryItemProcessor processor,
                                    RepositoryItemWriter<LogiflowTelemetry> writer,
                                    TaskExecutor taskExecutor) {
        return new StepBuilder("importTelemetryStep", jobRepository)
                .<TelemetryCsvRecord, LogiflowTelemetry>chunk(500, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(taskExecutor)
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skip(Exception.class)
                .skipLimit(500)
                .build();
    }

    @Bean
    public Job importTelemetryJob(JobRepository jobRepository, Step importTelemetryStep) {
        return new JobBuilder("importTelemetryJob", jobRepository)
                .start(importTelemetryStep)
                .build();
    }
}