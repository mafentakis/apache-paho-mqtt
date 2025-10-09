FROM mcr.microsoft.com/devcontainers/java:1-21-bullseye

ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        mosquitto-clients \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*
