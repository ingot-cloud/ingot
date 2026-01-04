#!/usr/bin/env bash

# 客户端版本信息配置
SERVICE_NAME=ingot-gateway
IMAGE_NAME=${REGISTRY_URL}/ingot/gateway:${GATEWAY_VERSION}

# 停止运行当前容器
docker ps -q --filter name="${SERVICE_NAME}" | xargs -r docker rm -f
# 删除当前镜像
docker images -q --filter reference="${IMAGE_NAME}" | xargs -r docker rmi -f
# 拉取最新镜像
docker pull ${IMAGE_NAME}

./standalone-deploy.sh start ${SERVICE_NAME}