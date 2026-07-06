# Compilación con Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Ejecución ligera
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Hardening: Creación de usuario no-root y carpeta temporal para el ETL
RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /app/uploads \
    && chown -R spring:spring /app

USER spring:spring

# Copia el .jar generado de la etapa anterior
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

# Comando de arranque
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]