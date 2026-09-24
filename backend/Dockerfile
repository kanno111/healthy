# syntax=docker/dockerfile:1

# Stage 1: compile the Maven multi-module project with Java 21.
FROM public.ecr.aws/docker/library/maven:3.9.16-eclipse-temurin-21-noble AS builder

WORKDIR /workspace
COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl healthy-server -am package -DskipTests \
    && cp healthy-server/target/healthy-server-*.jar /workspace/app.jar

# Stage 2: run the application with a smaller JRE-only image.
FROM public.ecr.aws/docker/library/eclipse-temurin:21.0.12_8-jre-noble

WORKDIR /app

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app app

COPY --from=builder --chown=app:app /workspace/app.jar /app/app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
