# Etape 1: Build (Maven)
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app
# Nsobou el pom.xml kbal bch naamlou cache lel dependencies
COPY pom.xml .
RUN mvn dependency:go-offline
# Nsobou l'code source w naamlou el build final
COPY src ./src
RUN mvn clean package -DskipTests

# Etape 2: Run (Java JRE) - Image sghira
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Nhezou ken el .jar li sna3neh mel etape loula
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
