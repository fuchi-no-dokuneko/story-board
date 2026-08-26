FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./mvnw --batch-mode -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
RUN command -v keytool >/dev/null \
    && useradd --create-home --uid 10001 storyblock
WORKDIR /app
COPY --chmod=500 scripts/container-entrypoint.sh /app/container-entrypoint.sh
COPY --chmod=500 scripts/generate-self-signed-tls.sh /app/generate-self-signed-tls.sh
RUN mkdir -p /app/data /app/tls/private /app/tls/public \
    && chown -R storyblock:storyblock /app
USER storyblock
ENTRYPOINT ["/app/container-entrypoint.sh"]

FROM runtime AS api
COPY --from=build --chown=storyblock:storyblock /workspace/apps/api/target/storyblock-api-*.jar /app/application.jar
EXPOSE 8443

FROM runtime AS style-worker
COPY --from=build --chown=storyblock:storyblock /workspace/apps/style-worker/target/storyblock-style-worker-*.jar /app/application.jar

FROM runtime AS llm-worker
COPY --from=build --chown=storyblock:storyblock /workspace/apps/llm-worker/target/storyblock-llm-worker-*.jar /app/application.jar
