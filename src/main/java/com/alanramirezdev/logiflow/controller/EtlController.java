package com.alanramirezdev.logiflow.controller;

import com.alanramirezdev.logiflow.entity.LogiflowTelemetry;
import com.alanramirezdev.logiflow.repository.LogiflowTelemetryRepository;
import com.alanramirezdev.logiflow.service.EtlService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/etl")
@CrossOrigin(origins = "*") // Conexión con frontend
public class EtlController {

    private final EtlService etlService;
    private final LogiflowTelemetryRepository repository;
    private final JobExplorer jobExplorer;

    public EtlController(EtlService etlService, LogiflowTelemetryRepository repository, JobExplorer jobExplorer) {
        this.etlService = etlService;
        this.repository = repository;
        this.jobExplorer = jobExplorer;
    }

    // Metodo auxiliar para verificar si hay un lote corriendo
    private boolean isJobRunning() {
        Set<JobExecution> executions = jobExplorer.findRunningJobExecutions("importTelemetryJob");
        return !executions.isEmpty();
    }

    // Endpoint principal
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadCsv(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();

        // Control de concurrencia: Se bloquea si ya hay un proceso activo
        if (isJobRunning()) {
            response.put("error", "Conflicto: Ya existe un proceso de ingesta masiva en ejecución. Por favor espere.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        if (file.isEmpty()) {
            response.put("error", "El archivo está vacío");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Proceso en segundo plano
        etlService.startEtlJob(file);

        // Generar ID de seguimiento simulado para la UI y respuesta inmediata
        String jobId = UUID.randomUUID().toString();
        response.put("message", "Archivo recibido. Procesamiento asíncrono iniciado.");
        response.put("jobId", jobId);

        // Retornar un 202 aunque el procesamiento no ha terminado
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    // Endpoint de soporte (Modo Demo)
    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, String>> resetDatabase() {

        if (isJobRunning()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "No se puede reiniciar la base de datos mientras un lote está en proceso.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        repository.deleteAll();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Base de datos reiniciada correctamente. Lista para nueva demo.");

        return ResponseEntity.ok(response);
    }

    // Endpoint para consultar el progreso
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getProgressStatus() {
        long currentCount = repository.count();
        Map<String, Object> response = new HashMap<>();
        response.put("processedRecords", currentCount);
        return ResponseEntity.ok(response);
    }

    // Endpoint para la Terminal de Logs
    @GetMapping("/preview")
    public ResponseEntity<List<LogiflowTelemetry>> getPreviewData() {
        List<LogiflowTelemetry> recentRecords = repository.findTop5ByOrderByIdDesc();
        return ResponseEntity.ok(recentRecords);
    }
}