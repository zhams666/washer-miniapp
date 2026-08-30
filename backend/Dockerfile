FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=build /workspace/target/washer-backend-*.jar /app/washer-backend.jar

ENV SPRING_PROFILES_ACTIVE=cloudbase \
    SERVER_ADDRESS=0.0.0.0 \
    SERVER_PORT=8080

EXPOSE 8080

USER spring

ENTRYPOINT ["java", "-jar", "/app/washer-backend.jar"]
