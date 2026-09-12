FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./mvnw --batch-mode -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /workspace
RUN useradd --no-create-home --uid 10001 storyblock \
    && mkdir -p /workspace/.local \
    && chown -R storyblock:storyblock /workspace
COPY --chmod=555 scripts/container-entrypoint.sh /workspace/entrypoint.sh
USER storyblock
ENV JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true -Djava.io.tmpdir=/workspace/.local/tmp -Dstoryblock.root=/workspace"
ENTRYPOINT ["/workspace/entrypoint.sh"]

FROM runtime AS api
COPY --from=build /workspace/server/target/storyblock-api-*.jar /workspace/application.jar
COPY server/config /workspace/server/config
ENV STORYBLOCK_CONTAINER_APP=api
EXPOSE 8443

FROM runtime AS style-worker
COPY --from=build /workspace/client-style-worker/target/storyblock-style-worker-*.jar /workspace/application.jar
ENV STORYBLOCK_CONTAINER_APP=style-worker

FROM runtime AS llm-worker
COPY --from=build /workspace/client-llm-worker/target/storyblock-llm-worker-*.jar /workspace/application.jar
ENV STORYBLOCK_CONTAINER_APP=llm-worker
