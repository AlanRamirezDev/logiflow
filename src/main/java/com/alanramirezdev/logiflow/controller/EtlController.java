package com.alanramirezdev.logiflow.controller;

import com.alanramirezdev.logiflow.entity.LogiflowTelemetry;
import com.alanramirezdev.logiflow.repository.LogiflowTelemetryRepository;
import com.alanramirezdev.logiflow.service.EtlService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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
     * Metodo auxiliar para extraer el idioma directamente de la cabecera
     */
    private boolean isEnglish() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return "en".equals(request.getHeader("Accept-Language"));
        }
        return false;
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
        boolean en = isEnglish();

        /**
         * Previene peticiones simultáneas
         */
        if (!initializationLock.compareAndSet(false, true)) {
            response.put("error", en
                    ? "Conflict: The server is already processing a concurrent request."
                    : "Conflicto: El servidor ya está procesando una solicitud concurrente.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        try {
            if (isJobRunning()) {
                response.put("error", en
                        ? "Conflict: A massive ingestion process is already running. Please wait."
                        : "Conflicto: Ya existe un proceso de ingesta masiva en ejecución. Por favor espere.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            if (file.isEmpty()) {
                response.put("error", en ? "The file is empty" : "El archivo está vacío");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            /**
             * Proceso en segundo plano
             */
            etlService.startEtlJob(file);

            String jobId = UUID.randomUUID().toString();
            response.put("message", en
                    ? "File received. Asynchronous processing started."
                    : "Archivo recibido. Procesamiento asíncrono iniciado.");
            response.put("jobId", jobId);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            response.put("error", en
                    ? "I/O Conflict. The file system is busy."
                    : "Conflicto en I/O. El sistema de archivos está ocupado.");
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
        boolean en = isEnglish();

        if (isJobRunning()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", en
                            ? "Denied: The engine is still processing data in the background. Please wait a few seconds for the current batch to finish before resetting."
                            : "Denegado: El motor sigue procesando datos en segundo plano. Por favor espere unos segundos a que finalice el lote actual antes de reiniciar.")
            );
        }

        repository.deleteAll();

        try {
            jdbcTemplate.execute("TRUNCATE TABLE BATCH_JOB_EXECUTION_CONTEXT, BATCH_JOB_EXECUTION_PARAMS, BATCH_STEP_EXECUTION_CONTEXT, BATCH_STEP_EXECUTION, BATCH_JOB_EXECUTION, BATCH_JOB_INSTANCE CASCADE;");
        } catch (Exception e) {
            System.err.println("Advertencia al purgar metadatos de Batch: " + e.getMessage());
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", en
                ? "Hard Reset successful. Database and internal metadata reset."
                : "Hard Reset exitoso. Base de datos y metadatos internos reiniciados.");

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
        boolean en = isEnglish();
        String sql = payload.get("query");

        if (sql == null || sql.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", en ? "Empty query." : "Consulta vacía."));
        }

        String upperSql = sql.toUpperCase().trim();

        if (!upperSql.startsWith("SELECT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", en
                            ? "Server security block: Only read queries (SELECT) are allowed."
                            : "Bloqueo de seguridad del servidor: Solo se permiten consultas de lectura (SELECT)."));
        }

        if (sql.indexOf(';') != -1 && sql.indexOf(';') != sql.trim().length() - 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", en
                            ? "[SECURITY ALERT] Stacking multiple statements is not allowed."
                            : "[SECURITY ALERT] No se permite apilar múltiples sentencias."));
        }

        String[] forbiddenKeywords = {"INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "TRUNCATE", "GRANT", "REVOKE", "COMMIT", "ROLLBACK", "EXEC", "UNION", "SLEEP", "PG_SLEEP"};
        for (String keyword : forbiddenKeywords) {
            if (upperSql.matches(".*\\b" + keyword + "\\b.*")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", en
                                ? "[SECURITY ALERT] Possible SQL injection detected. Forbidden word: " + keyword
                                : "[SECURITY ALERT] Posible inyección SQL detectada. Palabra prohibida: " + keyword));
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
                    .body(Map.of("error", en
                            ? "SQL syntax error in PostgreSQL: " + e.getMessage()
                            : "Error de sintaxis SQL en PostgreSQL: " + e.getMessage()));
        }
    }
}