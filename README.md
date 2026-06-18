# Logiflow ETL Motor - Backend

Este proyecto es un motor de procesamiento de datos ETL (Extract, Transform, Load) asíncrono y de alto rendimiento. Está diseñado para ingerir bitácoras masivas de datos, en este caso, flotillas logísticas, sin bloquear el hilo principal, garantizando una experiencia de usuario fluida.

## 🚀 Stack Tecnológico

* **Java 21:** Uso intensivo de **Virtual Threads (Project Loom)** para delegación de tareas I/O sin saturar el pool de hilos del sistema operativo.
* **Spring Boot 3:** Framework base para la API REST y la inyección de dependencias.
* **Spring Batch 5:** Orquestación del procesamiento masivo, lectura por fragmentos (*chunk-oriented processing*) y persistencia optimizada.
* **PostgreSQL:** Almacén de datos relacional.
* **Docker:** Contenerización del entorno de base de datos para paridad entre desarrollo y producción.

## 🧠 Decisiones Arquitectónicas y Patrones

1.  **Procesamiento Asíncrono:** El endpoint principal (`/api/v1/etl/upload`) responde inmediatamente con un `HTTP 202 Accepted` delegando el trabajo pesado a un `JobLauncher` asíncrono soportado por Hilos Virtuales.
2.  **Control de Concurrencia a nivel de Hilo:** Integración de `JobExplorer` para monitorear meta-tablas de Spring Batch. Si se detecta un Job en estado `STARTED`, el sistema bloquea nuevas peticiones con un `HTTP 409 Conflict`, previniendo condiciones de carrera y corrupción de datos.
3.  **Prevención de Inyecciones SQL (WAF):**
    El endpoint interactivo de consultas (`/api/v1/etl/query`) implementa una validación estricta de cadenas para bloquear comandos DML/DDL destructivos (DROP, TRUNCATE, UPDATE), forzando un entorno de solo lectura (`SELECT`) y aplicando límites de paginación para proteger la red.
4.  **Patrón Append-Only Logger (Demo):**
    Priorizando la velocidad de inserción sobre la deduplicación transaccional, el motor actual funciona como un *logger* crudo. Se registran las entradas sin validación de unicidad para peticiones POST repetidas del mismo archivo.

## ⚙️ Configuración y Ejecución Local

### Prerrequisitos
* Java 21 o superior.
* Maven.
* Docker y Docker Compose.

### Pasos
1.  Levantar la base de datos PostgreSQL mediante Docker:
    ```bash
    docker run --name logiflow-local-db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=logiflow_db -p 5432:5432 -d postgres:17
    ```
2.  Ejecutar la aplicación Spring Boot:
    ```bash
    ./mvnw spring-boot:run
    ```
El servidor se inicializará en `http://localhost:8081`.