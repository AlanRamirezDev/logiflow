package com.alanramirezdev.logiflow.batch;

import com.alanramirezdev.logiflow.dto.TelemetryCsvRecord;
import com.alanramirezdev.logiflow.entity.LogiflowTelemetry;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TelemetryItemProcessor implements ItemProcessor<TelemetryCsvRecord, LogiflowTelemetry> {

    // Formato de fecha para el mapeo
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public LogiflowTelemetry process(TelemetryCsvRecord item) throws Exception {

        if (item.getVehicleVin() == null || item.getVehicleVin().trim().isEmpty()) {
            return null;
        }

        try {
            return LogiflowTelemetry.builder()
                    .jobId("TEMP_LOTE_1")
                    .tripId(item.getTripId())
                    .vehicleVin(item.getVehicleVin())
                    .driverId(item.getDriverId())
                    .timestampUtc(LocalDateTime.parse(item.getTimestampUtc(), FORMATTER))
                    .odometerKm(Double.parseDouble(item.getOdometerKm()))
                    .fuelConsumedL(Double.parseDouble(item.getFuelConsumedL()))
                    .vehicleStatus(item.getVehicleStatus())
                    .routeCode(item.getRouteCode())
                    .build();

        } catch (Exception e) {
            System.err.println("Error procesando registro VIN " + item.getVehicleVin() + ": " + e.getMessage());
            return null;
        }
    }
}