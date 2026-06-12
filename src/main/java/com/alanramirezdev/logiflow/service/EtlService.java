package com.alanramirezdev.logiflow.service;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class EtlService {

    private final JobLauncher jobLauncher;
    private final Job importTelemetryJob;

    public EtlService(JobLauncher jobLauncher, Job importTelemetryJob) {
        this.jobLauncher = jobLauncher;
        this.importTelemetryJob = importTelemetryJob;
    }

    @Async
    public void startEtlJob(MultipartFile file) {
        try {
            Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path targetLocation = uploadDir.resolve("temp.csv");

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("startAt", System.currentTimeMillis())
                    .addString("originalFileName", file.getOriginalFilename())
                    .toJobParameters();

            System.out.println("Iniciando procesamiento asíncrono del archivo: " + file.getOriginalFilename());
            jobLauncher.run(importTelemetryJob, jobParameters);

        } catch (Exception e) {
            System.err.println("Error crítico al ejecutar el trabajo ETL: " + e.getMessage());
            e.printStackTrace();
        }
    }
}