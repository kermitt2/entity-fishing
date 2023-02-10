ARG ENTITY_FISHING_PORT=8090
ARG ENTITY_FISHING_PORT_MONITOR=8091
ARG BUILD_VERSION=0.0.6

# -------------
# builder image
# -------------
FROM openjdk:8u275-jdk as builder

USER root
ENV LANG="en_US.UTF-8" \
    LANGUAGE="en_US.UTF-8" \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

RUN apt-get update && \
    apt-get -y --no-install-recommends install unzip wget git 
# Final upgrade + clean
RUN apt-get update -y && \
    apt-get clean all -y

WORKDIR /opt/

# install GROBID
RUN wget --tries=10 --read-timeout=10 https://github.com/kermitt2/grobid/archive/refs/tags/0.7.2.zip
RUN unzip -o 0.7.2.zip && mv grobid-* grobid

WORKDIR /opt/grobid

# cleaning unused native libraries before packaging
RUN rm -rf grobid-home/pdf2xml
RUN rm -rf grobid-home/pdfalto/lin-32
RUN rm -rf grobid-home/pdfalto/mac-64
RUN rm -rf grobid-home/pdfalto/win-*
RUN rm -rf grobid-home/lib/lin-32
RUN rm -rf grobid-home/lib/win-*
RUN rm -rf grobid-home/lib/mac-64
RUN rm -rf ../0.7.2.zip

# cleaning DeLFT models
RUN rm -rf grobid-home/models/*-BidLSTM_CRF*

# building grobid
RUN ./gradlew clean assemble --no-daemon  --info --stacktrace
RUN chmod -R 755 /opt/grobid/grobid-home/pdfalto

# install grobid-ner
RUN git clone https://github.com/kermitt2/grobid-ner.git
WORKDIR /opt/grobid/grobid-ner
RUN ./gradlew copyModels
RUN ./gradlew clean install --no-daemon  --info --stacktrace

# install entity-fishing
WORKDIR /opt/

# gradle
COPY gradle/ ./entity-fishing/gradle/
COPY gradlew ./entity-fishing/
COPY gradle.properties ./entity-fishing/
COPY build.gradle ./entity-fishing/
COPY settings.gradle ./entity-fishing/

COPY data/ ./entity-fishing/data/
COPY lib/ ./entity-fishing/lib/
COPY src/ ./entity-fishing/src/

WORKDIR /opt/entity-fishing
RUN ./gradlew clean install -x test

# -------------
# runtime image
# -------------
FROM openjdk:11-jre-slim

RUN apt-get update && \
    apt-get -y --no-install-recommends install libxml2 libfontconfig htop nano
# Final upgrade + clean
RUN apt-get update -y && \
    apt-get clean all -y

WORKDIR /opt/grobid
COPY --from=builder /opt/grobid .

WORKDIR /opt/entity-fishing
COPY --from=builder /opt/entity-fishing .

# Hack m2 repository
COPY --from=builder /root/.m2/repository /root/.m2/repository
ADD lib/com /root/.m2/repository/com
ADD lib/fr /root/.m2/repository/fr
ADD lib/org /root/.m2/repository/or

# trigger gradle wrapper install
RUN chmod -R 755 /opt/entity-fishing/gradlew
RUN ./gradlew --no-daemon processResources classes dependencies -x compileJava

# More hack: Simulate gradlew in WORKDIR to get possible still missing prerequisites dependency files
# Add timeout + exit0 to prevent infinite interactive mode
RUN timeout 120s ./gradlew --no-daemon run -x compileJava || true

# Expose port ENTITY_FISHING_PORT (default 8090 and 8091)
EXPOSE ${ENTITY_FISHING_PORT}
EXPOSE ${ENTITY_FISHING_PORT_MONITOR}

# Start server (skip compile, we only have a JRE in this image)
# entity-fishing server will be accessible at http://<SERVER ADDRESS>:${ENTITY_FISHING_PORT}
CMD ["./gradlew", "--no-daemon", "run", "-x", "compileJava", "-x", "processResources", "-x", "classes"]

# Labels
LABEL \
    authors="The contributors" \
    org.label-schema.name="entity-fishing" \
    org.label-schema.description="Image for entity-fishing container" \
    org.label-schema.url="https://github.com/kermitt2/entity-fishing" \
    org.label-schema.version=${BUILD_VERSION}

# Thanks to Guillaume Karcher (guillaume.karcher@kairntech.com) for his help writing this dockerfile.
