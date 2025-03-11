FROM openjdk:21-jdk-slim
RUN addgroup spring && adduser --ingroup spring spring
USER root
RUN mkdir -p /images && chown spring:spring /images
USER spring:spring
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
