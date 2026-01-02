#!/usr/bin/env bash

source ./config.properties

# 客户端版本信息配置
VERSION=0.1.0
ServiceName=ingot-auth
ImageName=docker-registry.ingotcloud.top/ingot/auth:${VERSION}

# 停止运行当前容器
docker ps -q --filter name="${ServiceName}" | xargs -r docker rm -f
# 删除当前镜像
docker images -q --filter reference="${ImageName}" | xargs -r docker rmi -f
# 拉取最新镜像
docker pull ${ImageName}
# run
docker run -d --name ${ServiceName} --restart always \
    --memory=4g \
    --cpus=2 \
    --network ${DOCKER_NETWORK} --ip ${SERVICE_AUTH_HOST} \
    -e SERVICE_GATEWAY_HOST=${SERVICE_GATEWAY_HOST} \
    -e SERVICE_AUTH_HOST=${SERVICE_AUTH_HOST} \
    -e NACOS_SERVER_ADDR=${NACOS_SERVER_ADDR} \
    -e NACOS_USERNAME=${NACOS_USERNAME} \
    -e NACOS_PASSWORD=${NACOS_PASSWORD} \
    -e REDIS_HOST=${REDIS_HOST} \
    -e MYSQL_HOST=${MYSQL_HOST} \
    -e MYSQL_USERNAME=${MYSQL_USERNAME} \
    -e MYSQL_PASSWORD=${MYSQL_PASSWORD} \
    -e REDIS_PASSWORD=${REDIS_PASSWORD} \
    -e AUTH_JWK_MASTER_KEY=${AUTH_JWK_MASTER_KEY} \
    -v ${DOCKER_VOLUME}:${DOCKER_VOLUME} \
    ${ImageName}