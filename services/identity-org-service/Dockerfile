FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY shared-kernel ./shared-kernel
COPY identity-org-service ./identity-org-service
RUN mvn -B -pl shared-kernel,identity-org-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S zenius && adduser -S zenius -G zenius
USER zenius
COPY --from=build /workspace/identity-org-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

