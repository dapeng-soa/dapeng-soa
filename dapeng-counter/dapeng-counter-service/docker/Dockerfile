FROM dapengsoa/dapeng-container:2.0.5
MAINTAINER dapengsoa@gmail.com

ENV CONTAINER_HOME /dapeng-container
ENV PATH $CONTAINER_HOME:$PATH

RUN chmod +x ${CONTAINER_HOME}/bin/*.sh

WORKDIR ${CONTAINER_HOME}/bin

COPY dapeng-counter-service ${CONTAINER_HOME}/apps/dapeng-counter-service/

ENTRYPOINT ["./startup.sh"]