#!/usr/bin/env bash

# 中间件Host配置
MYSQL_HOST=172.88.0.10
NACOS_HOST=172.88.0.20
NACOS_PORT=8848
REDIS_HOST=172.88.0.90

# Ingot Host配置
INGOT_AUTH_SERVER=172.88.0.110
INGOT_GATEWAY=172.88.0.100

# 账号配置
MYSQL_USERNAME=root
MYSQL_PASSWORD=123456
REDIS_PASSWORD=123456

# 客户端版本信息配置
version=0.1.0
serviceName=ingot-pms-${version}
imageName=docker-registry.wangchao.im/ingot/pms:${version}

# 停止运行当前容器
docker ps -q --filter name="${serviceName}" | xargs -r docker rm -f
# 删除当前镜像
docker images -q --filter reference="${imageName}" | xargs -r docker rmi -f
# 拉取最新镜像
docker pull ${imageName}
# run
docker run -d --name ${serviceName} --restart always \
    --network ingot-net \
    --add-host ingot-db-redis:${REDIS_HOST} \
    --add-host ingot-db-mysql:${MYSQL_HOST} \
    --add-host ingot-auth-server:${INGOT_AUTH_SERVER} \
    --add-host ingot-gateway:${INGOT_GATEWAY} \
    -e NACOS_HOST=${NACOS_HOST} \
    -e NACOS_PORT=${NACOS_PORT} \
    -e REDIS_HOST=${REDIS_HOST} \
    -e MYSQL_HOST=${MYSQL_HOST} \
    -e MYSQL_USERNAME=${MYSQL_USERNAME} \
    -e MYSQL_PASSWORD=${MYSQL_PASSWORD} \
    -e REDIS_PASSWORD=${REDIS_PASSWORD} \
    -v /ingot-data:/ingot-data \
    ${imageName}