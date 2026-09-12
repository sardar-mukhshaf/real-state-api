FROM eclipse-temurin:26-jdk AS build

WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:26-jre

RUN useradd --system --uid 10001 --create-home realestate
WORKDIR /app
COPY --from=build /workspace/target/real-estate-backend-*.jar app.jar
RUN chown -R realestate:realestate /app
USER 10001

EXPOSE 3000 3001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]