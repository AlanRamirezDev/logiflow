package com.alanramirezdev.logiflow.repository;

import com.alanramirezdev.logiflow.entity.LogiflowTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogiflowTelemetryRepository extends JpaRepository<LogiflowTelemetry, Long> {

}