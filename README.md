🌐 **Read this in other language:** [English](README-en.md)

# 🚚 API Logiflow ETL - Motor de Ingesta Asíncrona

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Batch](https://img.shields.io/badge/Spring_Batch_5-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_17-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

¡Te doy la bienvenida al motor de procesamiento masivo de datos (ETL) de mi portafolio!

Este proyecto funciona como un microservicio asíncrono de alto rendimiento, diseñado para la extracción, transformación y carga masiva de bitácoras de telemetría logística. Resuelve el desafío arquitectónico de procesar decenas de miles de registros simultáneamente sin bloquear la interfaz de usuario, protegiendo al mismo tiempo la integridad de la base de datos frente a ataques y archivos corruptos.

## 🚀 Características del Proyecto & Arquitectura Backend

* **Concurrencia con Hilos Virtuales (Project Loom):** Delegación absoluta del trabajo I/O pesado a un `TaskExecutorAdapter` nativo de Java 21. Esto permite la ejecución de procesamiento paralelo real sin agotar el *pool* de hilos del sistema operativo, liberando la conexión HTTP instantáneamente (`202 Accepted`).
* **Inserciones Masivas JDBC (Bulk Inserts):** Sinergia total entre Spring Batch (`chunk(500)`), Hibernate (`allocationSize=500`, `batch_size`) y el Driver de PostgreSQL (`rewriteBatchedStatements`).
* **Defensa Multicapa (Race Conditions y WAF):** * Implementación de un semáforo atómico (`AtomicBoolean`) en memoria RAM para bloquear ataques de concurrencia y colisiones de I/O en la subida de archivos (vulnerabilidades TOCTOU).
    * Integración de un *Web Application Firewall* (WAF) simulado basado en RegEx y aislamiento `@Transactional(readOnly = true)` para neutralizar cualquier intento de Inyección SQL desde la terminal del cliente.
* **Resiliencia Fault-Tolerant:** Configuración estricta de políticas de omisión (`SkipPolicy`). Si el motor detecta registros corruptos, mal formados o vacíos, aísla y descarta la fila silenciosamente en lugar de abortar el lote, garantizando la continuidad de la operación.
* **Infraestructura Segura (IaC):** Despliegue empaquetado bajo *Multi-stage Build* sobre Alpine Linux. Incluye afinación de memoria (`-XX:MaxRAMPercentage`), mitigación de riesgos de escalado de privilegios operando bajo un usuario `spring` (no-root), y volumen Docker persistente para la retención segura de datos.

---

## 🛠️ Stack Tecnológico

| Tecnología | Versión | Propósito en el proyecto                                               |
| :--- | :--- |:-----------------------------------------------------------------------|
| **Spring Boot** | `^3.x` | Orquestación, controladores RESTful asíncronos e IoC                   |
| **Java** | `21` | Lenguaje principal (Hilos Virtuales y Records)                         |
| **Spring Batch** | `^5.x` | ETL para lectura de archivos (NIO) y procesamiento en Chunks           |
| **PostgreSQL** | `17` | Almacén relacional con capacidades de escritura por lotes y secuencias |
| **Docker** | `v2` | Contenerización y despliegue del entorno de red local                  |

---

## 💻 Comandos de Desarrollo y Despliegue

Instrucciones para levantar el entorno localmente. La API expone sus servicios en el puerto `8081` y requiere que Docker orqueste la base de datos aislada en el puerto reconfigurado `5435`.

| Comando | Acción |
| :--- | :--- |
| `docker-compose up -d` | Inicializa el contenedor PostgreSQL con volumen persistente |
| `mvn clean package` | Compila el código omitiendo tests y genera el artefacto ejecutable `.jar` |
| `mvn spring-boot:run` | Inicia la aplicación Spring Boot en el entorno local |

---

## 📡 Documentación de la API (Endpoints)

La API opera bajo la ruta base `/api/v1/etl`. Las respuestas fallidas retornan una estructura estándar `{"error": "Motivo del fallo"}`.

### Motor de Ingesta y Consulta
| Método | Endpoint | Descripción | Payload Requerido | Restricciones / Validaciones         |
| :--- | :--- | :--- | :--- |:-------------------------------------|
| `POST` | `/upload` | Inicia la ingesta asíncrona del archivo en disco e inicia el Hilo Virtual. | `MultipartFile` (CSV) | Límite 50MB. Devuelve `202 Accepted` |
| `GET`  | `/status` | Método de *Polling* para consultar el avance del Job Batch en tiempo real. | Ninguno | Público / Solo Lectura               |
| `GET`  | `/preview` | Retorna los 5 registros logísticos más recientes basados en el índice. | Ninguno | Público / Solo Lectura               |

### Consola Interactiva y Soporte
| Método | Endpoint | Descripción | Payload Requerido | Restricciones / Validaciones |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/query` | Motor SQL directo para evaluación en la terminal interactiva del dashboard. | JSON con `query` | Filtro WAF Anti-Drop/Update/Delete. Paginación forzada (`LIMIT`). |
| `DELETE` | `/reset` | Purga total (Modo Demo). Limpia las tablas y los metadatos de Spring Batch. | Ninguno | Trunca el estado de la BD a cero. |

---