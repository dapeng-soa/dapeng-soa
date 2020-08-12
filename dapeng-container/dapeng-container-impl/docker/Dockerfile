FROM dapengsoa/openjdk:java8-with-arthas
MAINTAINER dapengsoa@gmail.com

# Setting Envirnoment
ENV CONTAINER_HOME /dapeng-container
ENV PATH $CONTAINER_HOME:$PATH

RUN set -xe \
 && apk --no-cache add ttf-dejavu fontconfig

RUN mkdir -p "$CONTAINER_HOME"

COPY dapeng-container "$CONTAINER_HOME"

WORKDIR "$CONTAINER_HOME/bin"

RUN chmod +x *.sh

