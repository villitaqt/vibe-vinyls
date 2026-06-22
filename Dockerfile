# ---------- Etapa 1: build con Maven ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cachea dependencias primero
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Compila y empaqueta (sin tests en la imagen; se corren en CI)
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Etapa 2: runtime solo con el JAR ----------
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# Usuario sin privilegios
RUN groupadd --system app && useradd --system --gid app app

COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
