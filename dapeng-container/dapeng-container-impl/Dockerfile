#soa-container:base
FROM docker.oa.isuwang.com:5000/system/openjdk:1.0
MAINTAINER ever@iplas.com.cn

# Setting Envirnoment
ENV CONTAINER_HOME /dapeng-container
ENV PATH $CONTAINER_HOME:$PATH

RUN mkdir -p "$CONTAINER_HOME"

COPY target/dapeng-container /dapeng-container

WORKDIR "$CONTAINER_HOME/bin"

COPY fluent-bit/fluent-bit.conf /opt/fluent-bit/etc/fluent-bit.conf

RUN chmod +x *.sh
