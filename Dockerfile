# Compilación con Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Ejecución ligera
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copia el .jar generado de la etapa anterior
COPY --from=build /app/target/*.jar app.jar
# Expone el puerto de Spring Boot
EXPOSE 8081
# Comando de arranque
ENTRYPOINT ["java", "-jar", "app.jar"]