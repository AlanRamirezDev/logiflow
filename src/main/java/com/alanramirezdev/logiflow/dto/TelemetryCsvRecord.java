package com.alanramirezdev.logiflow.dto;

import lombok.Data;

@Data
public class TelemetryCsvRecord {

    // Todos los campos se reciben como String inicialmente para evitar que un error de formato
    // en el CSV rompa la lectura completa del archivo. La validación se hará después.

    private String trackingId;
    private String vehicleId;
    private String eventTimestamp;
    private String status;
    private String latitude;
    private String longitude;
    private String temperatureCelsius;
}