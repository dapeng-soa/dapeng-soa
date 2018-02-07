FROM dapengsoa/dapeng-container:2.0.0
MAINTAINER dapengsoa@gmail.com

ENV CONTAINER_HOME /dapeng-container
ENV PATH $CONTAINER_HOME:$PATH

RUN mkdir -p "$CONTAINER_HOME"

COPY startup.sh ${CONTAINER_HOME}/bin/

RUN chmod +x ${CONTAINER_HOME}/bin/*.sh

WORKDIR ${CONTAINER_HOME}/bin

RUN mkdir -p /data/wordVec

COPY dapeng-counter-service ${CONTAINER_HOME}/apps/dapeng-counter-service/

CMD ${CONTAINER_HOME}/bin/startup.sh && tail -F ${CONTAINER_HOME}/bin/startup.sh


