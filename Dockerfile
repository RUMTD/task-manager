# ============================================================
# Dockerfile multi-stage pour Task Manager Spring Boot
# Stage 1: Build  |  Stage 2: Runtime léger (JRE seulement)
# ============================================================

# --- STAGE 1: BUILD ---
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copie du pom.xml séparément pour exploiter le cache Docker
# Si seul le code source change, les dépendances ne sont pas re-téléchargées
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copie du code source et compilation
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- STAGE 2: RUNTIME ---
FROM eclipse-temurin:17-jre-alpine

# Bonnes pratiques sécurité: exécution en utilisateur non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copie uniquement du JAR compilé depuis le stage builder
COPY --from=builder /app/target/*.jar app.jar

# Changement vers l'utilisateur non-root
USER appuser

# Port exposé par Spring Boot
EXPOSE 8080

# Variables d'environnement avec valeurs par défaut
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
ENV SPRING_PROFILES_ACTIVE="default"

# Health check intégré
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
