package com.alanramirezdev.logiflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class EtlService {

    private final JobLauncher jobLauncher;
    private final Job importTelemetryJob;

    public EtlService(JobLauncher jobLauncher, Job importTelemetryJob) {
        this.jobLauncher = jobLauncher;
        this.importTelemetryJob = importTelemetryJob;
    }

    /**
     * Ingesta de archivos híbrida
     */
    public void startEtlJob(MultipartFile file) {
        try {
            Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path targetLocation = uploadDir.resolve("temp.csv");

            /**
             * Operación síncrona en el hilo principal
             */
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Archivo base {} transferido exitosamente a disco ({} bytes).",
                    file.getOriginalFilename(), file.getSize());

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("startAt", System.currentTimeMillis())
                    .addString("originalFileName", file.getOriginalFilename())
                    .addString("filePath", targetLocation.toString())
                    .toJobParameters();

            /**
             * Delegación a hilo virtual secundario para no bloquear la respuesta HTTP
             */
            log.info("Iniciando motor en segundo plano para: {}", file.getOriginalFilename());

            CompletableFuture.runAsync(() -> {
                try {
                    jobLauncher.run(importTelemetryJob, jobParameters);
                } catch (Exception e) {
                    log.error("Error crítico en la ejecución del Pipeline Batch: {}", e.getMessage(), e);
                }
            });

        } catch (Exception e) {
            log.error("Fallo estructural al preparar la ingesta de datos: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo iniciar el proceso ETL debido a un error de I/O.", e);
        }
    }
}