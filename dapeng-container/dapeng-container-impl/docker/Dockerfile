#soa-container:base
FROM docker.today36524.com.cn:5000/base/openjdk:server-jre8
#FROM docker.today36524.com.cn:5000/base/openjdk:jdk-11
MAINTAINER dapengsoa@gmail.com

# Setting Envirnoment
ENV CONTAINER_HOME /dapeng-container
ENV PATH $CONTAINER_HOME:$PATH

RUN mkdir -p "$CONTAINER_HOME"

COPY dapeng-container /dapeng-container

WORKDIR "$CONTAINER_HOME/bin"

RUN chmod +x *.sh
