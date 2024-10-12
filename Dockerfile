# Use a imagem oficial do Maven com Java 17 para criar o artefato de build.
# https://hub.docker.com/_/maven
FROM maven:3-eclipse-temurin-17-slim AS build-env

# Define o diretório de trabalho como /app
WORKDIR /app

# Copia o arquivo pom.xml para a imagem e baixa as dependências necessárias
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copia o código-fonte para a imagem
COPY src ./src

# Compila e empacota a aplicação, ignorando os testes
RUN mvn package -DskipTests

# Usa o OpenJDK 17 como imagem base para produção
# https://hub.docker.com/_/openjdk
FROM eclipse-temurin:17-jre-slim

# Copia o arquivo .jar da fase de build para a imagem de produção
COPY --from=build-env /app/target/*.jar /app/app.jar

# Define o comando para iniciar a aplicação
CMD ["java", "-jar", "/app/app.jar"]
