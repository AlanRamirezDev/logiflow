package com.alanramirezdev.logiflow.batch;

import com.alanramirezdev.logiflow.dto.TelemetryCsvRecord;
import com.alanramirezdev.logiflow.entity.LogiflowTelemetry;
import com.alanramirezdev.logiflow.repository.LogiflowTelemetryRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {

    // LECTOR:
    @Bean
    public FlatFileItemReader<TelemetryCsvRecord> reader() {
        return new FlatFileItemReaderBuilder<TelemetryCsvRecord>()
                .name("telemetryCsvReader")
                // Se lee de un archivo temporal en disco. El controlador guardará el archivo aquí.
                .resource(new FileSystemResource("uploads/temp.csv"))
                .linesToSkip(1) // Sin cabecera
                .delimited()
                // Nombramiento de columnas
                .names("trackingId", "vehicleId", "eventTimestamp", "status", "latitude", "longitude", "temperatureCelsius")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(TelemetryCsvRecord.class);
                }})
                .build();
    }

    // ESCRITOR:
    @Bean
    public RepositoryItemWriter<LogiflowTelemetry> writer(LogiflowTelemetryRepository repository) {
        return new RepositoryItemWriterBuilder<LogiflowTelemetry>()
                .repository(repository)
                .methodName("save")
                .build();
    }

    // STEP
    @Bean
    public Step importTelemetryStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    FlatFileItemReader<TelemetryCsvRecord> reader,
                                    TelemetryItemProcessor processor,
                                    RepositoryItemWriter<LogiflowTelemetry> writer) {
        return new StepBuilder("importTelemetryStep", jobRepository)
                // Solo bloques de 500 registros.
                .<TelemetryCsvRecord, LogiflowTelemetry>chunk(500, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    // JOB
    @Bean
    public Job importTelemetryJob(JobRepository jobRepository, Step importTelemetryStep) {
        return new JobBuilder("importTelemetryJob", jobRepository)
                .start(importTelemetryStep)
                .build();
    }
}