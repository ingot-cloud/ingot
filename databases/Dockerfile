FROM mysql/mysql-server:8.0.32

MAINTAINER ingotcloud(magician.of.technique@aliyun.com)

ENV TZ=Asia/Shanghai

RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY ./schema.sql /docker-entrypoint-initdb.d

COPY ./ingot_core.sql /docker-entrypoint-initdb.d

COPY ./ingot_nacos_config.sql /docker-entrypoint-initdb.d

COPY ./create_user.sql /docker-entrypoint-initdb.d

