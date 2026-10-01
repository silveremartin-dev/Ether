# ==============================================================================
# Ether — Cliodynamic & Historical Planetary Simulation Engine
# Multi-stage Dockerfile: Maven build → Eclipse Temurin 21 JRE Alpine runtime
#
# Supported MODE values (set via environment variable):
#   headless  — One-shot batch simulation run (default)
#   master    — Cluster master node (orchestrates workers)
#   worker    — Cluster compute worker node (connects to master)
#
# Copyright (c) 2024-2026 Silvère Martin-Michiellot
# ==============================================================================

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first to leverage Docker layer caching for dependency resolution.
# Maven dependencies are only re-downloaded when pom.xml changes.
COPY pom.xml .

# Pre-fetch all dependencies (offline-friendly subsequent builds)
RUN mvn dependency:go-offline -q || true

# Copy full source tree and build the executable fat-jar
COPY src ./src
COPY scripts ./scripts
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Silvère Martin-Michiellot <silvere.martin@gmail.com>"
LABEL description="Ether — Cliodynamic & Historical Planetary Simulation Engine"
LABEL version="1.0.0-beta.1"
LABEL org.opencontainers.image.source="https://github.com/silveremartin-dev/Ether"

WORKDIR /app

# Copy the self-contained executable jar from the build stage
COPY --from=build /app/target/society-simulation-1.0.0-beta.1-executable.jar ether.jar

# Persistent volumes: saves/snapshots, logs, and geospatial data
VOLUME ["/app/saves", "/app/logs", "/app/data"]

# Cluster RPC port (master ↔ worker socket communication)
EXPOSE 9090

# ── Environment defaults ──────────────────────────────────────────────────────
# MODE: execution mode — headless | master | worker
ENV MODE=headless
# SCENARIO: simulation scenario preset (e.g. OUT_OF_AFRICA, MESOPOTAMIA_BRONZE_AGE)
ENV SCENARIO=OUT_OF_AFRICA
# TICKS: number of simulation ticks to execute
ENV TICKS=500
# CELLS: number of H3 spatial grid cells
ENV CELLS=5000
# Cluster networking (used in master and worker modes)
ENV MASTER_HOST=ether-master
ENV MASTER_PORT=9090
ENV CLUSTER_SECRET=EtherClusterSecret2026
# PostgreSQL connection (optional — Ether falls back to H2 in-memory if not set)
ENV DB_HOST=postgres
ENV DB_PORT=5432
ENV DB_NAME=ether_simulation
ENV DB_USER=ether
ENV DB_PASSWORD=change_me_in_production
# Redis connection (optional)
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379
# Ether directory paths (12-Factor App configurable storage)
ENV ETHER_DATA_DIR=/app/data
ENV ETHER_SAVES_DIR=/app/saves
ENV ETHER_LOGS_DIR=/app/logs
ENV ETHER_USER_PRESETS_DIR=/app/saves/user_presets
ENV ETHER_CACHE_DIR=/tmp/ether_cache

# Health check: verify the JVM process is running
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD pgrep -f "ether.jar" > /dev/null || exit 1

# ── Entrypoint: mode-dispatching shell ────────────────────────────────────────
# Dispatches to the correct simulation mode based on the MODE environment variable.
# JVM flags:
#   -Xms2g / -Xmx10g   : heap sizing for large H3 cell arrays
#   -XX:+UseG1GC        : low-pause garbage collector for long-running simulations
#   --add-modules ...   : incubator Vector API for SIMD-accelerated cell processing
ENTRYPOINT ["/bin/sh", "-c", "\
  JVM_ARGS=\"-Xms2g -Xmx10g -XX:+UseG1GC --add-modules jdk.incubator.vector\"; \
  case \"${MODE}\" in \
    headless) \
      echo \"[Ether] Starting headless batch run — scenario=${SCENARIO} ticks=${TICKS} cells=${CELLS}\"; \
      exec java ${JVM_ARGS} -jar ether.jar \
        --headless \
        --scenario=\"${SCENARIO}\" \
        --ticks=\"${TICKS}\" \
        --cells=\"${CELLS}\" \
        --profile ;; \
    master) \
      echo \"[Ether] Starting cluster MASTER — port=${MASTER_PORT} scenario=${SCENARIO}\"; \
      exec java ${JVM_ARGS} -jar ether.jar \
        --headless \
        --mode=cluster --role=master \
        --port=\"${MASTER_PORT}\" \
        --secret=\"${CLUSTER_SECRET}\" \
        --scenario=\"${SCENARIO}\" \
        --ticks=\"${TICKS}\" \
        --cells=\"${CELLS}\" \
        --profile ;; \
    worker) \
      echo \"[Ether] Starting cluster WORKER — master=${MASTER_HOST}:${MASTER_PORT}\"; \
      exec java ${JVM_ARGS} -jar ether.jar \
        --headless \
        --mode=cluster --role=worker \
        --master-host=\"${MASTER_HOST}\" \
        --port=\"${MASTER_PORT}\" \
        --secret=\"${CLUSTER_SECRET}\" ;; \
    *) \
      echo \"[Ether] ERROR: Unknown MODE='${MODE}'. Valid values: headless | master | worker\"; \
      exit 1 ;; \
  esac \
"]
