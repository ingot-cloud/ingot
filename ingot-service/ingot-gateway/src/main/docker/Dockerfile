FROM alibabadragonwell/dragonwell:17-anolis

VOLUME ["/ingot-data"]

ADD ingot-gateway.jar app.jar

RUN sh -c 'touch ./app.jar'

RUN mkdir -p /ingot-data

ENV TZ=Asia/Shanghai JAVA_OPTS="-Xms128m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

CMD ["java", "$JAVA_OPTS", "-jar", "app.jar"]
