FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
COPY build/libs/*.jar worker.jar
ENTRYPOINT ["java","-jar","/worker.jar"]