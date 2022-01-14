FROM anapsix/alpine-java:8_server-jre_unlimited

VOLUME ["/data/ingot"]

ADD ingot-pms-provider.jar app.jar

RUN sh -c 'touch ./app.jar'

RUN mkdir -p /data/ingot

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -jar app.jar
