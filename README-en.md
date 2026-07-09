🌐 **Leer en otro idioma:** [Español](README.md)

# 🚚 Logiflow ETL API - Asynchronous Ingestion Engine

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Batch](https://img.shields.io/badge/Spring_Batch_5-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_17-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

Welcome to the massive data processing (ETL) engine of my portfolio!

This project functions as a high-performance asynchronous microservice designed for the extraction, transformation, and massive loading of logistics telemetry logs. It solves the architectural challenge of processing tens of thousands of records simultaneously without blocking the user interface, while safeguarding database integrity against attacks and corrupted files.

## 🚀 Project Features & Backend Architecture

* **Concurrency with Virtual Threads (Project Loom):** Absolute delegation of heavy I/O workloads to a native Java 21 `TaskExecutorAdapter`. This enables real parallel processing execution without exhausting the operating system's thread pool, freeing the HTTP connection instantly (`202 Accepted`).
* **JDBC Bulk Inserts:** Total synergy between Spring Batch (`chunk(500)`), Hibernate (`allocationSize=500`, `batch_size`), and the PostgreSQL Driver (`rewriteBatchedStatements`).
* **Multi-layered Defense (Race Conditions & WAF):**
    * Implementation of an atomic semaphore (`AtomicBoolean`) in RAM to block concurrency attacks and I/O collisions during file uploads (TOCTOU vulnerabilities).
    * Integration of a simulated Web Application Firewall (WAF) based on RegEx and `@Transactional(readOnly = true)` isolation to neutralize any SQL Injection attempts from the client terminal.
* **Fault-Tolerant Resilience:** Strict configuration of skip policies (`SkipPolicy`). If the engine detects corrupted, malformed, or empty records, it isolates and silently discards the row instead of aborting the batch, guaranteeing operational continuity.
* **Secure Infrastructure (IaC):** Packaged deployment using Multi-stage Builds over Alpine Linux. Includes memory tuning (`-XX:MaxRAMPercentage`), mitigation of privilege escalation risks by operating under a non-root `spring` user, and a persistent Docker volume for secure data retention.

---

## 🛠️ Tech Stack

| Technology | Version | Purpose in the project |
| :--- | :--- |:-----------------------------------------------------------------------|
| **Spring Boot** | `^3.x` | Orchestration, asynchronous RESTful controllers, and IoC |
| **Java** | `21` | Main language (Virtual Threads and Records) |
| **Spring Batch** | `^5.x` | ETL for file reading (NIO) and Chunk processing |
| **PostgreSQL** | `17` | Relational store with batch writing capabilities and sequences |
| **Docker** | `v2` | Containerization and local network environment deployment |

---

## 💻 Development and Deployment Commands

Instructions to boot the environment locally. The API exposes its services on port `8081` and requires Docker to orchestrate the isolated database on the reconfigured port `5435`.

| Command | Action |
| :--- | :--- |
| `docker-compose up -d` | Initializes the PostgreSQL container with a persistent volume |
| `mvn clean package` | Compiles code skipping tests and generates the executable `.jar` artifact |
| `mvn spring-boot:run` | Starts the Spring Boot application in the local environment |

---

## 📡 API Documentation (Endpoints)

The API operates under the `/api/v1/etl` base path. Failed responses return a standardized structure: `{"error": "Failure reason"}`.

### Ingestion Engine & Queries
| Method | Endpoint | Description | Required Payload | Restrictions / Validations |
| :--- | :--- | :--- | :--- |:-------------------------------------|
| `POST` | `/upload` | Starts the asynchronous file ingestion on disk and spins up the Virtual Thread. | `MultipartFile` (CSV) | 50MB limit. Returns `202 Accepted` |
| `GET`  | `/status` | Polling method to query the Batch Job progress in real-time. | None | Public / Read-Only |
| `GET`  | `/preview` | Returns the 5 most recent logistics records based on index. | None | Public / Read-Only |

### Interactive Console & Support
| Method | Endpoint | Description | Required Payload | Restrictions / Validations |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/query` | Direct SQL engine for evaluation in the dashboard's interactive terminal. | JSON with `query` | Anti-Drop/Update/Delete WAF filter. Forced pagination (`LIMIT`). |
| `DELETE` | `/reset` | Total purge (Demo Mode). Clears tables and Spring Batch internal metadata. | None | Truncates database state back to zero. |

---