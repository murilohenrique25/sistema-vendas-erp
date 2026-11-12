FROM maven:3.9.11-eclipse-temurin-21 AS build
ENV MAVEN_OPTS="-Xmx512m"
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B package -DskipTests
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S erp && adduser -S erp -G erp
USER erp
WORKDIR /app
COPY --from=build /app/target/erp-backend-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=45s --retries=6 CMD wget -q -O /dev/null "http://127.0.0.1:${SERVER_PORT:-18080}/actuator/health" || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
