#!/usr/bin/env bash

source ./config.env

# 域名配置
VIRTUAL_HOST=ingot-api.ingotcloud.top
VIRTUAL_PORT=7980

# 客户端版本信息配置
version=0.1.0
serviceName=ingot-gateway-${version}
imageName=docker-registry.ingotcloud.top/ingot/gateway:${version}

# 停止运行当前容器
docker ps -q --filter name="${serviceName}" | xargs -r docker rm -f
# 删除当前镜像
docker images -q --filter reference="${imageName}" | xargs -r docker rmi -f
# 拉取最新镜像
docker pull ${imageName}
# run
docker run -d --name ${serviceName} --restart always \
    --network ingot-net --ip ${INGOT_GATEWAY} \
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
    -e REDIS_PASSWORD=${REDIS_PASSWORD} \
    -e VIRTUAL_HOST=${VIRTUAL_HOST} \
    -e VIRTUAL_PORT=${VIRTUAL_PORT} \
    -e LETSENCRYPT_HOST=${VIRTUAL_HOST} \
    -v /ingot-data:/ingot-data \
    ${imageName}