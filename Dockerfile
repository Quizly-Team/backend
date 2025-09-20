FROM openjdk:17-jdk-slim AS build
WORKDIR /app
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && sed -i 's/\r$//' ./gradlew && ./gradlew dependencies
COPY . .
RUN ./gradlew bootJar

FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
