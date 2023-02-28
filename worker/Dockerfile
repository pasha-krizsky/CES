FROM openjdk:17-jdk-slim
VOLUME /tmp
COPY build/libs/*.jar worker.jar
ENTRYPOINT ["java","-jar","/worker.jar"]