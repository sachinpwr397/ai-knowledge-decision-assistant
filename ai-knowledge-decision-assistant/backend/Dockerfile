FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -r -u 10001 appuser && mkdir -p /app/data/uploads && chown -R appuser:appuser /app
COPY --from=build /workspace/target/ai-knowledge-assistant-1.0.0.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
