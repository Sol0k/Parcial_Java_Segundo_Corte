FROM gradle:8.14-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle bootWar --no-daemon
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/app.war app.war
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]