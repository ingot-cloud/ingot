FROM alibabadragonwell/dragonwell:17-anolis

VOLUME ["/ingot-data"]

ADD ingot-pms-provider.jar app.jar

RUN sh -c 'touch ./app.jar'

RUN mkdir -p /ingot-data

ENV TZ=Asia/Shanghai JAVA_OPTS="-Xms128m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

# 时区
#RUN apk add tzdata && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
#    && echo "Asia/Shanghai" > /etc/timezone \
#    && apk del tzdata

CMD sleep 10; java $JAVA_OPTS -jar app.jar
#ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -jar app.jar
