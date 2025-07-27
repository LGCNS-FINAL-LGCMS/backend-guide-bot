FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
EXPOSE 8080
COPY build/libs/*.jar /app/app.jar
ENTRYPOINT java -jar /app/app.jar
