package com.alanramirezdev.logiflow.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Se opta por utilizar anotaciones granulares de Lombok en lugar de @Data para evitar la
 * generación de métodos equals() y hashCode()
 */
@Getter
@Setter
@ToString
public class TelemetryCsvRecord {

    /**
     * Todos los campos se mantienen como String para garantizar la supervivencia del Reader
     * ante archivos corruptos.
     */
    private String tripId;
    private String vehicleVin;
    private String driverId;
    private String timestampUtc;
    private String odometerKm;
    private String fuelConsumedL;
    private String vehicleStatus;
    private String routeCode;
}