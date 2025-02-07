# Builder image
FROM ubuntu:24.04 AS base

ARG CI_COMMIT_REF_NAME=Unreleased
ARG CI_COMMIT_SHA=#
ARG GROUP_ID=0
ARG USER_ID=0

# specify wanted versions of Java and SBT
ENV CI_COMMIT_REF_NAME=$CI_COMMIT_REF_NAME \
    CI_COMMIT_SHA=$CI_COMMIT_SHA \
    DEBIAN_FRONTEND=noninteractive \
    HOME=/src \
    SBT_VERSION=1.10.0

WORKDIR ${HOME}

# install Java and curl
RUN apt-get update > /dev/null && apt-get install apt-utils -y > /dev/null && apt-get install --no-install-recommends openjdk-17-jdk curl gnupg -y > /dev/null

# install sbt
RUN curl -fsSL https://scala.jfrog.io/artifactory/debian/sbt-${SBT_VERSION}.deb -o sbt.deb && dpkg -i sbt.deb

USER ${USER_ID}:${GROUP_ID}

# add the content of the portal app
FROM base AS build
COPY . ${HOME}

# SBT test and build
RUN sbt test
RUN sbt stage

# Runtime image
FROM ubuntu:24.04 AS runtime
RUN apt-get update > /dev/null && apt-get --no-install-recommends install openjdk-17-jdk -y > /dev/null && rm -rf /var/lib/apt/lists/*
COPY --from=build /src/target/universal/stage/ /opt/

# Make container runnable as non-root
RUN chgrp -R 0 /opt && chmod -R g=u /opt

ENTRYPOINT "/opt/bin/ldaplogin" $PLAY_START_PARAMETERS
