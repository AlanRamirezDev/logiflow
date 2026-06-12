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

    @Column(name = "trip_id")
    private String tripId;

    @Column(name = "vehicle_vin")
    private String vehicleVin;

    @Column(name = "driver_id")
    private String driverId;

    @Column(name = "timestamp_utc")
    private LocalDateTime timestampUtc;

    @Column(name = "odometer_km")
    private Double odometerKm;

    @Column(name = "fuel_consumed_l")
    private Double fuelConsumedL;

    @Column(name = "vehicle_status")
    private String vehicleStatus;

    @Column(name = "route_code")
    private String routeCode;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}