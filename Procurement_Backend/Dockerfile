# Builds and runs the Spring Boot API. Used by Render (see render.yaml in the repo root).

# ---------- 1. Build the jar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---------- 2. Run it ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/procurement-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p uploads

# Render tells the app which port to use in $PORT. Memory settings fit the free 512 MB instance.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
EXPOSE 8081
CMD ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8081} -jar app.jar"]
