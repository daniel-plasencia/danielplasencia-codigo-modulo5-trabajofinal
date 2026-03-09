FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/*.jar /app/notification-service.jar

EXPOSE 8086

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/notification-service.jar"]
