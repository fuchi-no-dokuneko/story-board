FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./mvnw --batch-mode -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --create-home --uid 10001 storyblock
WORKDIR /app
COPY --from=build /workspace/apps/api/target/storyblock-api-*.jar /app/storyblock-api.jar
RUN mkdir /app/data && chown -R storyblock:storyblock /app
USER storyblock
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "/app/storyblock-api.jar"]
