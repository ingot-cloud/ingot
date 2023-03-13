FROM anapsix/alpine-java:8_server-jre_unlimited

VOLUME ["/ingot-data"]

ADD ingot-auth.jar app.jar

RUN sh -c 'touch ./app.jar'

RUN mkdir -p /ingot-data

 # 设定时区
ENV TZ=Asia/Shanghai
RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -jar app.jar
