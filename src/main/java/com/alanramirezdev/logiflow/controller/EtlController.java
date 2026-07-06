package com.alanramirezdev.logiflow.controller;

import com.alanramirezdev.logiflow.entity.LogiflowTelemetry;
import com.alanramirezdev.logiflow.repository.LogiflowTelemetryRepository;
import com.alanramirezdev.logiflow.service.EtlService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/v1/etl")
@CrossOrigin(origins = "*")
public class EtlController {

    private final EtlService etlService;
    private final LogiflowTelemetryRepository repository;
    private final JobExplorer jobExplorer;
    private final JdbcTemplate jdbcTemplate;
    private final AtomicBoolean initializationLock = new AtomicBoolean(false);

    public EtlController(EtlService etlService, LogiflowTelemetryRepository repository, JobExplorer jobExplorer, JdbcTemplate jdbcTemplate) {
        this.etlService = etlService;
        this.repository = repository;
        this.jobExplorer = jobExplorer;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Metodo auxiliar para verificar si hay un lote corriendo
     */
    private boolean isJobRunning() {
        Set<JobExecution> executions = jobExplorer.findRunningJobExecutions("importTelemetryJob");
        return !executions.isEmpty();
    }

    /**
     * Endpoint principal
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadCsv(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();

        /**
         * Previene peticiones simultáneas
         */
        if (!initializationLock.compareAndSet(false, true)) {
            response.put("error", "Conflicto: El servidor ya está procesando una solicitud concurrente.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        try {
            if (isJobRunning()) {
                response.put("error", "Conflicto: Ya existe un proceso de ingesta masiva en ejecución. Por favor espere.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            if (file.isEmpty()) {
                response.put("error", "El archivo está vacío");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            /**
             * Proceso en segundo plano
             */
            etlService.startEtlJob(file);

            String jobId = UUID.randomUUID().toString();
            response.put("message", "Archivo recibido. Procesamiento asíncrono iniciado.");
            response.put("jobId", jobId);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            response.put("error", "Conflicto en I/O. El sistema de archivos está ocupado.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } finally {
            Thread.startVirtualThread(() -> {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                initializationLock.set(false);
            });
        }
    }

    /**
     * Endpoint de soporte (Modo Demo)
     */
    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, String>> resetDatabase() {
        repository.deleteAll();

        /**
         * Purgar el historial interno
         */
        try {
            jdbcTemplate.execute("TRUNCATE TABLE BATCH_JOB_EXECUTION_CONTEXT, BATCH_JOB_EXECUTION_PARAMS, BATCH_STEP_EXECUTION_CONTEXT, BATCH_STEP_EXECUTION, BATCH_JOB_EXECUTION, BATCH_JOB_INSTANCE CASCADE;");
        } catch (Exception e) {
            System.err.println("Advertencia al purgar metadatos de Batch: " + e.getMessage());
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Hard Reset exitoso. Base de datos y metadatos internos reiniciados.");

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para consultar el progreso y la salud del proceso
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getProgressStatus() {
        long currentCount = repository.count();
        Map<String, Object> response = new HashMap<>();

        response.put("processedRecords", currentCount);
        response.put("isRunning", isJobRunning());

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para la Terminal de Logs
     */
    @GetMapping("/preview")
    public ResponseEntity<List<LogiflowTelemetry>> getPreviewData() {
        List<LogiflowTelemetry> recentRecords = repository.findTop5ByOrderByIdDesc();
        return ResponseEntity.ok(recentRecords);
    }

    /**
     * Endpoint para consultas SQL reales desde la terminal
     */
    @PostMapping("/query")
    @Transactional(readOnly = true)
    public ResponseEntity<?> executeDynamicQuery(@RequestBody Map<String, String> payload) {
        String sql = payload.get("query");

        if (sql == null || sql.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Consulta vacía."));
        }

        String upperSql = sql.toUpperCase().trim();

        if (!upperSql.startsWith("SELECT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Bloqueo de seguridad del servidor: Solo se permiten consultas de lectura (SELECT)."));
        }

        if (sql.indexOf(';') != -1 && sql.indexOf(';') != sql.trim().length() - 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "[SECURITY ALERT] No se permite apilar múltiples sentencias."));
        }

        String[] forbiddenKeywords = {"INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "TRUNCATE", "GRANT", "REVOKE", "COMMIT", "ROLLBACK", "EXEC", "UNION"};
        for (String keyword : forbiddenKeywords) {
            if (upperSql.matches(".*\\b" + keyword + "\\b.*")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "[SECURITY ALERT] Posible inyección SQL detectada. Palabra prohibida: " + keyword));
            }
        }

        String finalSql = sql.trim();
        if (finalSql.endsWith(";")) {
            finalSql = finalSql.substring(0, finalSql.length() - 1);
        }

        /**
         * Se inyecta un LIMIT 15 de protección si el usuario hace un SELECT * masivo para no saturar la red
         */
        if (!upperSql.contains("LIMIT")) {
            finalSql = finalSql + " LIMIT 15";
        }

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(finalSql);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error de sintaxis SQL en PostgreSQL: " + e.getMessage()));
        }
    }
}