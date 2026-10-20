FROM tomcat:8.5-jdk8-temurin
USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && mkdir -p /usr/local/tomcat/webapps/ROOT/WEB-INF/lib \
    && curl -fsSL -o /usr/local/tomcat/webapps/ROOT/WEB-INF/lib/jstl-1.2.jar https://repo1.maven.org/maven2/javax/servlet/jstl/1.2/jstl-1.2.jar \
    && rm -rf /var/lib/apt/lists/*
RUN rm -rf /usr/local/tomcat/webapps/ROOT/index.jsp
COPY . /usr/local/tomcat/webapps/ROOT
EXPOSE 8080
CMD ["catalina.sh","run"]