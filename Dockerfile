FROM gradle:8.14-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle war --no-daemon
FROM tomcat:10.1-jdk21-temurin
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=build /app/build/libs/app.war /usr/local/tomcat/webapps/ROOT.war
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
EXPOSE 10000
ENTRYPOINT ["/entrypoint.sh"]