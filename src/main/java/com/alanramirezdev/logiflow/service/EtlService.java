package com.alanramirezdev.logiflow.service;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Service
public class EtlService {

    private final JobLauncher jobLauncher;
    private final Job importTelemetryJob;

    // Inyección de dependencias a través del constructor
    public EtlService(JobLauncher jobLauncher, Job importTelemetryJob) {
        this.jobLauncher = jobLauncher;
        this.importTelemetryJob = importTelemetryJob;
    }

    // Delegar tarea a hilo virtual.
    @Async
    public void startEtlJob(MultipartFile file) {
        try {
            File dir = new File("uploads");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File tempFile = new File(dir, "temp.csv");
            file.transferTo(tempFile);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("startAt", System.currentTimeMillis())
                    .addString("originalFileName", file.getOriginalFilename())
                    .toJobParameters();

            // Disparar el motor asíncrono
            System.out.println("Iniciando procesamiento asíncrono del archivo: " + file.getOriginalFilename());
            jobLauncher.run(importTelemetryJob, jobParameters);

        } catch (Exception e) {
            System.err.println("Error crítico al ejecutar el trabajo ETL: " + e.getMessage());
        }
    }
}