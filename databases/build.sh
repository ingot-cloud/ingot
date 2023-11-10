INNER_VERSION=0.1.0
MODULE_NAME=ingot-mysql

source ./.local_env

docker login -u ${JY_DOCKER_REGISTRY_USERNAME} -p ${JY_DOCKER_REGISTRY_PASSWORD} docker-registry.ingotcloud.top
docker build -t docker-registry.ingotcloud.top/ingot/${MODULE_NAME}:${INNER_VERSION} .
docker push docker-registry.ingotcloud.top/ingot/${MODULE_NAME}:${INNER_VERSION}
