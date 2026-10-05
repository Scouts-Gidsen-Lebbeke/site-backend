FROM eclipse-temurin:21-jdk-jammy
RUN addgroup spring && adduser --ingroup spring spring
USER root
RUN mkdir -p /images && chown spring:spring /images
RUN mkdir -p /logs
USER spring:spring
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
