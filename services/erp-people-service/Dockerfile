FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY shared-kernel ./shared-kernel
COPY erp-people-service ./erp-people-service
RUN mvn -B -pl shared-kernel,erp-people-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S zenius && adduser -S zenius -G zenius
USER zenius
COPY --from=build /workspace/erp-people-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

