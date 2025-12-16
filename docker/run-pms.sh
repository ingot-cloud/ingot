#!/usr/bin/env bash

source ./config.properties

# 客户端版本信息配置
VERSION=0.1.0
ServiceName=ingot-pms
ImageName=docker-registry.ingotcloud.top/ingot/pms:${VERSION}

# 停止运行当前容器
docker ps -q --filter name="${ServiceName}" | xargs -r docker rm -f
# 删除当前镜像
docker images -q --filter reference="${ImageName}" | xargs -r docker rmi -f
# 拉取最新镜像
docker pull ${ImageName}
# run
docker run -d --name ${ServiceName} --restart always \
    --network ${DOCKER_NETWORK} \
    -e SERVICE_GATEWAY_HOST=${SERVICE_GATEWAY_HOST} \
    -e SERVICE_AUTH_HOST=${SERVICE_AUTH_HOST} \
    -e NACOS_HOST=${NACOS_HOST} \
    -e NACOS_PORT=${NACOS_PORT} \
    -e NACOS_USERNAME=${NACOS_USERNAME} \
    -e NACOS_PASSWORD=${NACOS_PASSWORD} \
    -e REDIS_HOST=${REDIS_HOST} \
    -e MYSQL_HOST=${MYSQL_HOST} \
    -e MYSQL_USERNAME=${MYSQL_USERNAME} \
    -e MYSQL_PASSWORD=${MYSQL_PASSWORD} \
    -e REDIS_PASSWORD=${REDIS_PASSWORD} \
    -v ${DOCKER_VOLUME}:${DOCKER_VOLUME} \
    ${ImageName}