FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./mvnw --batch-mode -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
RUN useradd --create-home --uid 10001 storyblock
WORKDIR /app
COPY --chmod=500 scripts/container-entrypoint.sh /app/container-entrypoint.sh
RUN mkdir /app/data && chown -R storyblock:storyblock /app
USER storyblock
ENTRYPOINT ["/app/container-entrypoint.sh"]

FROM runtime AS api
COPY --from=build --chown=storyblock:storyblock /workspace/apps/api/target/storyblock-api-*.jar /app/application.jar
EXPOSE 8080

FROM runtime AS style-worker
COPY --from=build --chown=storyblock:storyblock /workspace/apps/style-worker/target/storyblock-style-worker-*.jar /app/application.jar

FROM runtime AS llm-worker
COPY --from=build --chown=storyblock:storyblock /workspace/apps/llm-worker/target/storyblock-llm-worker-*.jar /app/application.jar
