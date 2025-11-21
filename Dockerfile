FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY target/vu-light-agent-0.0.1-SNAPSHOT.jar app.jar

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

ENV SERVER_PORT=9001
EXPOSE 9001

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT}/health || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]


