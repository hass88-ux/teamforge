FROM node:22-bookworm-slim AS frontend
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /build
COPY backend/ backend/
COPY --from=frontend /build/frontend/dist/ frontend/dist/
RUN mvn -B -f backend/pom.xml -Prelease -DskipTests package

FROM eclipse-temurin:21-jre-noble
RUN apt-get update && apt-get install -y --no-install-recommends python3 python3-venv \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --uid 10001 teamforge
WORKDIR /app
COPY ai/requirements.txt /app/requirements.txt
RUN python3 -m venv /opt/recommendations && /opt/recommendations/bin/pip install --no-cache-dir -r /app/requirements.txt
COPY ai/teamforge_ai/ /app/ai/teamforge_ai/
COPY --from=backend /build/backend/target/teamforge-api-0.1.0-SNAPSHOT.jar /app/teamforge.jar
COPY scripts/container-start.py /app/container-start.py
RUN chown -R teamforge:teamforge /app
USER teamforge
ENV SERVER_ADDRESS=0.0.0.0 AI_SERVICE_URL=http://127.0.0.1:8001 PYTHONUNBUFFERED=1
EXPOSE 8080
CMD ["/opt/recommendations/bin/python", "/app/container-start.py"]
