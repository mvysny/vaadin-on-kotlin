# Allows you to run this app easily as a docker container.
# See README.md for more details.
#
# 1. Build the image with: docker build -t test/vok-example-crud:latest .
# 2. Run the image with: docker run --rm -ti -p8080:8080 test/vok-example-crud
#
# Uses Docker Multi-stage builds: https://docs.docker.com/build/building/multi-stage/

# The "Build" stage. Copies the entire project into the container, into the /app/ folder, and builds it.
FROM eclipse-temurin:17 AS BUILD
COPY . /app/
WORKDIR /app/
RUN --mount=type=cache,target=/root/.gradle --mount=type=cache,target=/root/.vaadin rm -rf .gradle/caches/modules-2/modules-2.lock .gradle/caches/*/plugin-resolution .gradle/caches/journal-1 && .gradle/ ./gradlew clean build -Pvaadin.productionMode --no-daemon --info --stacktrace
WORKDIR /app/vok-example-crud/build/distributions/
RUN ls -la
RUN tar xvf vok-example-crud-*.tar && rm vok-example-crud-*.tar && rm vok-example-crud-*.zip
RUN ls -la
# At this point, we have the app (executable bash scrip plus a bunch of jars) in the
# /app/vok-example-crud/build/distributions/vok-example-crud/ folder.

# The "Run" stage. Start with a clean image, and copy over just the app itself, omitting gradle, npm and any intermediate build files.
FROM openjdk:21-bookworm
COPY --from=BUILD /app/vok-example-crud/build/distributions/vok-example-crud-* /app/
WORKDIR /app/bin
EXPOSE 8080
ENTRYPOINT ./vok-example-crud
