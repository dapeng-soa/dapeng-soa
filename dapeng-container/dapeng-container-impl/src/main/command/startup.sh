#!/bin/sh

export JVM_HOME='opt/oracle-server-jre'
export PATH=$JVM_HOME/bin:$PATH

PRGNAME=soa-service
ADATE=`date +%Y%m%d%H%M%S`
PRGDIR=`pwd`
dirname $0|grep "^/" >/dev/null
if [ $? -eq 0 ];then
   PRGDIR=`dirname $0`
else
    dirname $0|grep "^\." >/dev/null
    retval=$?
    if [ $retval -eq 0 ];then
        PRGDIR=`dirname $0|sed "s#^.#$PRGDIR#"`
    else
        PRGDIR=`dirname $0|sed "s#^#$PRGDIR/#"`
    fi
fi

LOGDIR=$PRGDIR/../logs
if [ ! -d "$LOGDIR" ]; then
        mkdir "$LOGDIR"
fi

CLASSPATH=$PRGDIR/classes

filelist=`find $PRGDIR/lib/* -type f -name "*.jar"`
for filename in $filelist
do
  CLASSPATH=$CLASSPATH:$filename
done

#DEBUG="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9997"
JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=1091 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
JVM_OPTS=" -Xms256M -Xmx256M -Dfile.encoding=UTF-8 -Dsun.jun.encoding=UTF-8"
NETTY_OPTS=" -Dio.netty.leakDetectionLevel=advanced "
GC_OPTS=" -XX:NewRatio=1 -XX:SurvivorRatio=30 -XX:+UseParallelGC -XX:+UseParallelOldGC -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDateStamps -Xloggc:$LOGDIR/gc-$PRGNAME-$ADATE.log -XX:+PrintGCDetails -Dlog.dir=$PRGDIR/.."
SOA_BASE="-Dsoa.base=$PRGDIR/../ -Dsoa.run.mode=native"

# SIGTERM-handler  graceful-shutdown
pid=0
process_exit() {
 if [ $pid -ne 0 ]; then
  kill -SIGTERM "$pid"
  wait "$pid"
 fi
 exit 143; # 128 + 15 -- SIGTERM
}


trap 'kill ${!};process_exit' SIGTERM

nohup java -server $JVM_OPTS $GC_OPTS $NETTY_OPTS $SOA_BASE $DEBUG_OPTS $USER_OPTS  $E_JAVA_OPTS -cp ./dapeng-bootstrap.jar com.github.dapeng.bootstrap.Bootstrap >> $LOGDIR/console.log 2>&1 &
pid="$!"
echo $pid > $LOGDIR/pid.txt

fluentBitEnable = "$fluent_bit_enable"
if [ "$fluentBitEnable" == "" ]; then
    fluentBitEnable="false"
fi

if [ "$fluentBitEnable" == "true" ]; then
   nohup sh /opt/fluent-bit/fluent-bit.sh >> $LOGDIR/fluent-bit.log 2>&1 &
fi

wait $pid
