# ---- Build stage: compila el JAR con Maven + Corretto 21 ----
FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Runtime stage: solo el JRE y el JAR ----
FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /build/target/web-framework.jar app.jar

# Configuracion por variables de entorno (se pueden sobrescribir con -e)
ENV PORT=8080 \
    APP_ENV=production \
    WORKER_THREADS=32 \
    SHUTDOWN_TIMEOUT_SECONDS=8

EXPOSE 8080

# Forma "exec": java es el PID 1 y recibe SIGTERM directamente de `docker stop`,
# lo que dispara el apagado ordenado del servidor.
ENTRYPOINT ["java", "-jar", "app.jar"]
