# Etapa 1: Build (Compilar o código)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copia os arquivos do projeto para dentro do container
COPY pom.xml .
COPY src ./src

# Roda o maven para compilar ignorando os testes
RUN mvn clean package -DskipTests

# Etapa 2: Imagem final (Apenas para rodar)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia O ARQUIVO .jar da Etapa 1 para esta imagem final
COPY --from=build /app/target/*.jar api.jar

# Expõe a porta que a API usa
EXPOSE 8080

# Comando para iniciar a aplicação
ENTRYPOINT ["java", "-jar", "api.jar"]
