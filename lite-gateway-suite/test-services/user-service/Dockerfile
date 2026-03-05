FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY target/user-service-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 9001

ENTRYPOINT ["java", "-jar", "app.jar"]
