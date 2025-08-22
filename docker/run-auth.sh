#!/usr/bin/env bash

source ./config.env

# 客户端版本信息配置
version=0.1.0
serviceName=ingot-auth-${version}
imageName=docker-registry.ingotcloud.top/ingot/auth:${version}

# 停止运行当前容器
docker ps -q --filter name="${serviceName}" | xargs -r docker rm -f
# 删除当前镜像
docker images -q --filter reference="${imageName}" | xargs -r docker rmi -f
# 拉取最新镜像
docker pull ${imageName}
# run
docker run -d --name ${serviceName} --restart always \
    --network ingot-net --ip ${INGOT_AUTH_SERVER} \
    --add-host ingot-db-redis:${REDIS_HOST} \
    --add-host ingot-db-mysql:${MYSQL_HOST} \
    --add-host ingot-auth-server:${INGOT_AUTH_SERVER} \
    --add-host ingot-gateway:${INGOT_GATEWAY} \
    -e NACOS_HOST1=${NACOS_HOST1} \
    -e NACOS_PORT1=${NACOS_PORT1} \
    -e NACOS_HOST2=${NACOS_HOST2} \
    -e NACOS_PORT2=${NACOS_PORT2} \
    -e NACOS_HOST3=${NACOS_HOST3} \
    -e NACOS_PORT3=${NACOS_PORT3} \
    -e NACOS_USERNAME=${NACOS_USERNAME} \
    -e NACOS_PASSWORD=${NACOS_PASSWORD} \
    -e REDIS_HOST=${REDIS_HOST} \
    -e MYSQL_HOST=${MYSQL_HOST} \
    -e MYSQL_USERNAME=${MYSQL_USERNAME} \
    -e MYSQL_PASSWORD=${MYSQL_PASSWORD} \
    -e REDIS_PASSWORD=${REDIS_PASSWORD} \
    -v /ingot-data:/ingot-data \
    ${imageName}