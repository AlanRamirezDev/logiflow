package com.alanramirezdev.logiflow.batch;

import com.alanramirezdev.logiflow.dto.TelemetryCsvRecord;
import com.alanramirezdev.logiflow.entity.LogiflowTelemetry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@StepScope
public class TelemetryItemProcessor implements ItemProcessor<TelemetryCsvRecord, LogiflowTelemetry> {

    /**
     * Formato de fecha para el mapeo
     */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Inyección dinámica del nombre original del archivo desde los metadatos del Job
     */
    @Value("#{jobParameters['originalFileName']}")
    private String sourceFileName;

    @Override
    public LogiflowTelemetry process(TelemetryCsvRecord item) throws Exception {

        /**
         * Si el VIN viene vacío descarta el registro (no cuenta como error).
         */
        if (item.getVehicleVin() == null || item.getVehicleVin().trim().isEmpty()) {
            return null;
        }

        return LogiflowTelemetry.builder()
                .jobId(sourceFileName != null ? sourceFileName : "LOTE_DESCONOCIDO")
                .tripId(item.getTripId() != null ? item.getTripId().trim() : null)
                .vehicleVin(item.getVehicleVin().trim())
                .driverId(item.getDriverId() != null ? item.getDriverId().trim() : null)
                .timestampUtc(LocalDateTime.parse(item.getTimestampUtc().trim(), FORMATTER))
                .odometerKm(Double.parseDouble(item.getOdometerKm().trim()))
                .fuelConsumedL(Double.parseDouble(item.getFuelConsumedL().trim()))
                .vehicleStatus(item.getVehicleStatus() != null ? item.getVehicleStatus().trim() : null)
                .routeCode(item.getRouteCode() != null ? item.getRouteCode().trim() : null)
                .build();
    }
}