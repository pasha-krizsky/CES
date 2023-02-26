FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
COPY build/libs/server-all.jar server.jar
ENTRYPOINT ["java","-jar","/server.jar"]