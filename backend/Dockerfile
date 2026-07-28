# syntax=docker.m.daocloud.io/docker/dockerfile:1.7

ARG BASE_REGISTRY=docker.m.daocloud.io

FROM ${BASE_REGISTRY}/library/node:20-alpine AS web-build
WORKDIR /workspace
COPY h5/package.json h5/package-lock.json ./h5/
RUN cd h5 && npm ci --registry=https://registry.npmmirror.com
COPY h5 ./h5
RUN mkdir -p backend/src/main/resources/static/m \
    && cd h5 \
    && npm run build

FROM ${BASE_REGISTRY}/library/maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY backend/pom.xml .
COPY backend/src ./src
COPY --from=web-build /workspace/backend/src/main/resources/static/m ./src/main/resources/static/m
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B clean package -DskipTests

FROM ${BASE_REGISTRY}/library/eclipse-temurin:21-jre-alpine
WORKDIR /app

# 打包 FFmpeg（含 ffprobe），供曲库扫描解析媒体。
# Intel VAAPI requires the userspace iHD driver in addition to FFmpeg's VAAPI encoders.
# intel-media-driver is x86_64-only, so keep the multi-architecture ARM64 image unchanged.
RUN sed -i 's#https://dl-cdn.alpinelinux.org#https://mirrors.aliyun.com#g' /etc/apk/repositories \
    && for attempt in 1 2 3; do \
      apk add --no-cache ffmpeg && break; \
      if [ "$attempt" -eq 3 ]; then exit 1; fi; \
      sleep 5; \
    done \
    && if [ "$(apk --print-arch)" = "x86_64" ]; then \
      apk add --no-cache intel-media-driver libva-utils; \
    fi

COPY --from=build /build/target/home-ktv-server.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
