# Usa a imagem base do Eclipse Temurin com JDK 17
FROM eclipse-temurin:17-jdk-jammy

# Define o diretório de trabalho como /app
WORKDIR /app

# Copia o diretório .mvn e o arquivo pom.xml para a imagem
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Dá permissão de execução para o script mvnw
RUN chmod +x ./mvnw

# Converte as quebras de linha do mvnw durante o build (caso necessário)
RUN apt-get update && apt-get install -y dos2unix && dos2unix ./mvnw

RUN dos2UNIX ./mvnw

# Resolve as dependências do projeto
RUN ./mvnw dependency:resolve

# Copia todo o código-fonte para a imagem
COPY src ./src

# Comando para executar a aplicação usando Maven
CMD ["./mvnw", "spring-boot:run"]
