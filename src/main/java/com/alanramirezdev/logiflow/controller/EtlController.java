package com.alanramirezdev.logiflow.controller;

import com.alanramirezdev.logiflow.repository.LogiflowTelemetryRepository;
import com.alanramirezdev.logiflow.service.EtlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/etl")
@CrossOrigin(origins = "*") // Conexión con frontend
public class EtlController {

    private final EtlService etlService;
    private final LogiflowTelemetryRepository repository;

    public EtlController(EtlService etlService, LogiflowTelemetryRepository repository) {
        this.etlService = etlService;
        this.repository = repository;
    }

    // Endpoint principal
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadCsv(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();

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
        repository.deleteAll();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Base de datos reiniciada correctamente. Lista para nueva demo.");

        return ResponseEntity.ok(response);
    }
}