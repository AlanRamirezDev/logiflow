package com.alanramirezdev.logiflow.dto;

import lombok.Data;

@Data
public class TelemetryCsvRecord {
    // Todos los campos se reciben como String inicialmente para evitar que un error de formato
    // en el CSV rompa la lectura completa del archivo. La validación se hará después.
    private String tripId;
    private String vehicleVin;
    private String driverId;
    private String timestampUtc;
    private String odometerKm;
    private String fuelConsumedL;
    private String vehicleStatus;
    private String routeCode;
}