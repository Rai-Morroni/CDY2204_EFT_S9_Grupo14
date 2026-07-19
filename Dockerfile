# ============================================================
# Etapa 1: build con Maven
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Se copian primero pom.xml para aprovechar la cache de capas de Docker
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ============================================================
# Etapa 2: imagen final, liviana, solo con el jar y el Wallet
# ============================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiamos el .jar desde la Etapa 1
COPY --from=build /app/target/guia-despacho-backend.jar app.jar

# Copiamos la carpeta del Wallet de Oracle desde tu computadora al contenedor
# IMPORTANTE: Asegúrate de tener la carpeta descargada en la raíz de tu proyecto
COPY Wallet_MI-BD/ /app/Wallet_MI-BD/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]