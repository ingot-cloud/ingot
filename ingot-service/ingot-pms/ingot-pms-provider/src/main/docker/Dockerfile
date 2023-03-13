FROM anapsix/alpine-java:8_server-jre_unlimited

VOLUME ["/ingot-data"]

ADD ingot-pms-provider.jar app.jar

RUN sh -c 'touch ./app.jar'

RUN mkdir -p /ingot-data

# 时区
RUN apk add tzdata && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -jar app.jar
