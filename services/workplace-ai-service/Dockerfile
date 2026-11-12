FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY shared-kernel ./shared-kernel
COPY workplace-ai-service ./workplace-ai-service
RUN mvn -B -pl shared-kernel,workplace-ai-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S zenius && adduser -S zenius -G zenius
USER zenius
COPY --from=build /workspace/workplace-ai-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

