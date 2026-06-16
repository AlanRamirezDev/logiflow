package com.alanramirezdev.logiflow.repository;

import com.alanramirezdev.logiflow.entity.LogiflowTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LogiflowTelemetryRepository extends JpaRepository<LogiflowTelemetry, Long> {

    // Metodo que construye automáticamente por el nombre
    List<LogiflowTelemetry> findTop5ByOrderByIdDesc();

}