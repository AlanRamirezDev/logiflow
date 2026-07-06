package com.alanramirezdev.logiflow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Se remueve @Data para evitar la generación insegura de equals/hashCode en contextos JPA.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "logiflow_telemetry",
        indexes = {
                @Index(name = "idx_telemetry_status", columnList = "vehicle_status"),
                @Index(name = "idx_telemetry_vin", columnList = "vehicle_vin")
        }
)
public class LogiflowTelemetry {

    @Id
    /* *
     * Habilita el JDBC Batching
     */
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "telemetry_seq")
    @SequenceGenerator(name = "telemetry_seq", sequenceName = "logiflow_telemetry_seq", allocationSize = 500)
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