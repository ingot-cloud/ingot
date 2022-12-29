FROM anapsix/alpine-java:8_server-jre_unlimited

VOLUME ["/ingot-data"]

ADD ingot-gateway.jar app.jar

RUN sh -c 'touch ./app.jar'

RUN mkdir -p /ingot-data

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -jar app.jar
