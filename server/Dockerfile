FROM openjdk:17-jdk-slim
VOLUME /tmp
COPY build/libs/server-all.jar server.jar
ENTRYPOINT ["java","-jar","/server.jar"]