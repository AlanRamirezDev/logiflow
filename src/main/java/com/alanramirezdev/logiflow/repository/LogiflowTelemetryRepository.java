package com.alanramirezdev.logiflow.repository;

import com.alanramirezdev.logiflow.entity.LogiflowTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LogiflowTelemetryRepository extends JpaRepository<LogiflowTelemetry, Long> {

    /**
     * Recupera los últimos 5 registros insertados para la vista previa del frontend.
     */
    List<LogiflowTelemetry> findTop5ByOrderByIdDesc();

}