#soa-container:base
FROM docker.oa.isuwang.com:5000/system/jdk8:2.0
MAINTAINER ever@iplas.com.cn

# Setting Envirnoment
ENV CONTAINER_HOME /dapeng-container
ENV PATH $CONTAINER_HOME:$PATH

RUN mkdir -p "$CONTAINER_HOME"

COPY target/dapeng-container /dapeng-container

WORKDIR "$CONTAINER_HOME/bin"

RUN chmod +x *.sh