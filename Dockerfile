# THE READER AND THE SWEEP ARE THE SAME IMAGE, RUN TWO WAYS.
#
# One build, because they share every class: the sweep walks chapters and writes readings, the
# reader serves what is on disk. Two images would be two chances for the server to answer with a
# vocabulary the writer no longer produces.
#
# ratchet is not on Maven Central and does not deploy anywhere — it ships a source tree and tags.
# So this clones it at the version the pom pins and installs from source, which is what
# install-ratchet.sh does on a laptop. The clone is its own layer: it changes when the pin changes
# and not when our code does.

# THE INTERFACE IS BUILT HERE AND SERVED BY JAVA, so nothing node-shaped reaches the runtime image.
# The reader is already an HTTP server with the record mounted into it; giving it a static directory
# costs one handler, where a Node process beside it would be a second thing to keep alive and a
# second place for the dashboard to be dark exactly when the sweep it reports on has died. That is
# the same argument the compose file makes for splitting the reader from the sweep.
#
# ratchet-ui is a git dependency at a tag, for the reason ratchet is: neither publishes to a
# registry. pnpm needs the lockfile and the manifest before the sources, so a change to a component
# does not re-resolve the tree.
FROM node:22-alpine AS ui
WORKDIR /ui
RUN corepack enable
COPY ui/package.json ui/pnpm-lock.yaml* ./
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm install --no-frozen-lockfile
COPY ui/ ./
RUN pnpm build

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src

# The pin, alone, so a code change does not re-clone and re-build ratchet.
COPY pom.xml install-ratchet.sh ./
RUN set -eu; \
    version=$(sed -n 's|.*<ratchet.version>\(.*\)</ratchet.version>.*|\1|p' pom.xml | head -1); \
    echo "warandpeace: installing ratchet $version from source"; \
    git clone --depth 1 --branch "v$version" https://github.com/vasiliy-mikhailov/ratchet.git /tmp/ratchet; \
    cd /tmp/ratchet && mvn -B -q -DskipTests install; \
    rm -rf /tmp/ratchet

# Dependencies before sources, for the same reason.
RUN mvn -B -q dependency:go-offline || true

COPY app ./app
RUN mvn -B -q -DskipTests package && mvn -B -q dependency:build-classpath -Dmdep.outputFile=cp.txt

FROM eclipse-temurin:17-jre
WORKDIR /app

# THE CORPUS SHIPS IN THE IMAGE. 3.4 MB of chapters, and the alternative is a volume that can be
# absent or stale — a run against a corpus nobody can identify is a run nobody can repeat.
COPY corpus ./corpus
COPY gold ./gold
COPY --from=ui /ui/dist ./static
COPY --from=build /src/target/classes ./classes
COPY --from=build /src/cp.txt ./cp.txt
COPY --from=build /root/.m2/repository /root/.m2/repository

# Readings, the record and the rendered wiki are volumes: they outlive the container by design,
# because a killed run picking up where it stopped is the whole point of the library underneath.
VOLUME ["/app/readings", "/app/records", "/app/wiki"]

EXPOSE 8092
ENV WP_PORT=8092

# The reader by default. The sweep overrides the command.
CMD ["sh", "-c", "java -cp classes:$(cat cp.txt) tech.mikhailov.wp.Dash $WP_PORT"]
