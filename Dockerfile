# syntax=docker/dockerfile:1
ARG JAVA_VERSION=26

FROM maven:3.9-eclipse-temurin-${JAVA_VERSION} AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package && mvn -B -q dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/lib

FROM build AS test
RUN mvn -B test

FROM eclipse-temurin:${JAVA_VERSION}-jre AS runtime
RUN useradd --system --uid 10001 --home-dir /data --shell /usr/sbin/nologin trajectorium \
    && mkdir -p /data && chown trajectorium:trajectorium /data
WORKDIR /app
COPY --from=build /build/target/lib ./lib
COPY --from=build /build/target/trajectorium-*jar ./app.jar

ENV TRAJECTORIUM_HOME=/data
USER trajectorium
VOLUME ["/datya"]

ENTRYPOINT ["java", "-Djava.awt.headless=true", "-cp", "app.jar:lib/*", "trajectorium.Launcher", "--headless"]
CMD ["--scene=preset:solar-system", "--years=1", "--format=txt"]