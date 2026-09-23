# ============================================================
# Etape 1 : construction de l application (Maven + JDK 25)
# ============================================================
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Cache des dependances : on copie d abord le POM seul
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Compilation et tests
COPY src ./src
RUN mvn -B clean package

# ============================================================
# Etape 2 : image d execution legere (JRE seul)
# ============================================================
FROM eclipse-temurin:25-jre
WORKDIR /app

# Utilisateur non privilégie
RUN useradd --system --uid 1001 appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

# La cle API est fournie a l execution via le .env ou l environnement :
# voir docker-compose.yml, NE PAS l inscrire ici.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
