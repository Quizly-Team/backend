FROM openjdk:17.0.2-jdk-slim
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
