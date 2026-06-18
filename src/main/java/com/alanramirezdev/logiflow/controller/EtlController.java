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
import org.springframework.jdbc.core.JdbcTemplate;

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
    private final JdbcTemplate jdbcTemplate;

    public EtlController(EtlService etlService, LogiflowTelemetryRepository repository, JobExplorer jobExplorer, JdbcTemplate jdbcTemplate) {
        this.etlService = etlService;
        this.repository = repository;
        this.jobExplorer = jobExplorer;
        this.jdbcTemplate = jdbcTemplate;
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

        // Limpieza de datos
        repository.deleteAll();

        // Purgar el historial interno
        try {
            jdbcTemplate.execute("TRUNCATE TABLE BATCH_JOB_EXECUTION_CONTEXT, BATCH_JOB_EXECUTION_PARAMS, BATCH_STEP_EXECUTION_CONTEXT, BATCH_STEP_EXECUTION, BATCH_JOB_EXECUTION, BATCH_JOB_INSTANCE CASCADE;");
        } catch (Exception e) {
            System.err.println("Advertencia al purgar metadatos de Batch: " + e.getMessage());
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Hard Reset exitoso. Base de datos y metadatos internos reiniciados.");

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

    // Endpoint para consultas SQL reales desde la terminal
    @PostMapping("/query")
    public ResponseEntity<?> executeDynamicQuery(@RequestBody Map<String, String> payload) {
        String sql = payload.get("query");

        if (sql == null || sql.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Consulta vacía."));
        }

        String upperSql = sql.toUpperCase().trim();

        // Segunda capa de seguridad: Rechaza cualquier cosa que no sea lectura
        if (!upperSql.startsWith("SELECT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Bloqueo de seguridad del servidor: Solo se permiten consultas de lectura (SELECT)."));
        }

        // Se inyecta un LIMIT 15 de protección si el usuario hace un SELECT * masivo para no saturar la red
        if (!upperSql.contains("LIMIT")) {
            sql = sql.replace(";", "") + " LIMIT 15;";
        }

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error de sintaxis SQL en PostgreSQL: " + e.getMessage()));
        }
    }
}