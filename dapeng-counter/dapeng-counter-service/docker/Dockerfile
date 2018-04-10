FROM dapengsoa/dapeng-container:2.0.2-SNAPSHOT
MAINTAINER dapengsoa@gmail.com

ENV CONTAINER_HOME /dapeng-container
ENV PATH $CONTAINER_HOME:$PATH

RUN chmod +x ${CONTAINER_HOME}/bin/*.sh

WORKDIR ${CONTAINER_HOME}/bin

COPY dapeng-counter-service ${CONTAINER_HOME}/apps/dapeng-counter-service/

CMD ${CONTAINER_HOME}/bin/startup.sh && tail -F ${CONTAINER_HOME}/bin/startup.sh


