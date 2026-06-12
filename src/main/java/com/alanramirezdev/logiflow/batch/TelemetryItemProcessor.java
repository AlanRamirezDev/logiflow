package com.alanramirezdev.logiflow.batch;

import com.alanramirezdev.logiflow.dto.TelemetryCsvRecord;
import com.alanramirezdev.logiflow.entity.LogiflowTelemetry;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TelemetryItemProcessor implements ItemProcessor<TelemetryCsvRecord, LogiflowTelemetry> {

    // Se deja en formato ISO 8601 por defecto
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    public LogiflowTelemetry process(TelemetryCsvRecord item) throws Exception {
        try {
            // Se transforman los "Strings" crudos del CSV en tipos de datos estrictos para la BD.
            return LogiflowTelemetry.builder()
                    .jobId("TEMP_LOTE_1")
                    .trackingId(item.getTrackingId())
                    .vehicleId(item.getVehicleId())
                    .eventTimestamp(LocalDateTime.parse(item.getEventTimestamp(), FORMATTER))
                    .status(item.getStatus())
                    .latitude(Double.parseDouble(item.getLatitude()))
                    .longitude(Double.parseDouble(item.getLongitude()))
                    .temperatureCelsius(Double.parseDouble(item.getTemperatureCelsius()))
                    .build();

        } catch (Exception e) {
            // Si el archivo viene con una letra donde va un número, el sistema capturará el error.
            System.err.println("Fila descartada por datos corruptos (Tracking ID: " + item.getTrackingId() + ")");
            return null;
        }
    }
}