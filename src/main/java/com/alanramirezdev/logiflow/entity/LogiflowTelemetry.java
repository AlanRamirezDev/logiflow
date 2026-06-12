package com.alanramirezdev.logiflow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "logiflow_telemetry")
public class LogiflowTelemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private String jobId;

    @Column(name = "tracking_id", nullable = false)
    private String trackingId;

    @Column(name = "vehicle_id", nullable = false)
    private String vehicleId;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @Column(nullable = false)
    private String status;

    private Double latitude;

    private Double longitude;

    @Column(name = "temperature_celsius")
    private Double temperatureCelsius;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Esta función se ejecuta automáticamente justo antes de guardar en BD
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}