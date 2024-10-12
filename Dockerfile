# Use a imagem oficial do Maven com Java 17 para criar o artefato de build
FROM maven:3-eclipse-temurin-17-slim AS build

# Define o diretório de trabalho como /app
WORKDIR /app

# Copia o arquivo pom.xml e as configurações do Maven para a imagem
COPY pom.xml ./

# Baixa as dependências para acelerar builds futuros
RUN mvn dependency:go-offline -B

# Copiatodo o código-fonte para a imagem
COPY src ./src

# Compila e empacota a aplicação, ignorando os testes
RUN mvn clean package -DskipTests

# Usa o OpenJDK 17 como imagem base para produção
FROM eclipse-temurin:17-jre-slim

# Cria um diretório para a aplicação
RUN mkdir /app

# Copia o arquivo .jar gerado da fase de build para o diretório de produção
COPY --from=build /app/target/restaurant-1.0.0.jar /app/restaurant-1.0.0.jar

# Expõe a porta 8080 para acesso à aplicação
EXPOSE 8080

# Define o comando para iniciar a aplicação
ENTRYPOINT ["java", "-jar", "/app/restaurant-1.0.0.jar"]
