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

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')

#DEBUG="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9997"
JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=1091 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
NETTY_OPTS=" -Dio.netty.leakDetectionLevel=advanced "
#GC_OPTS=" -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDateStamps -Xloggc:$LOGDIR/gc-$PRGNAME-$ADATE.log -XX:+PrintGCDetails -XX:+PrintPromotionFailure -XX:+PrintGCApplicationStoppedTime -Dlog.dir=$PRGDIR/.."


if [ "$JAVA_VERSION" \< "11" ]; then
    GC_OPTS=" -XX:NewRatio=1 -XX:SurvivorRatio=30 -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDateStamps -Xloggc:$LOGDIR/gc-$PRGNAME-$ADATE.log -XX:+PrintPromotionFailure -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCDetails -Dlog.dir=$PRGDIR/.. -XX:+UseParallelGC -XX:+UseParallelOldGC"
else
    GC_OPTS=" -XX:NewRatio=1 -XX:SurvivorRatio=30 -XX:+HeapDumpOnOutOfMemoryError -Xloggc:$LOGDIR/gc-$PRGNAME-$ADATE.log -XX:+PrintGCDetails -Dlog.dir=$PRGDIR/.. -XX:+UnlockExperimentalVMOptions -XX:+UseZGC"
fi



#预分配内存, 会造成jvm进程启动的时候慢一点, 但运行时减轻gc停顿, 减少内存碎片
MEM_OPTS="-XX:+AlwaysPreTouch"
#如果线程数较多，函数的递归较少，线程栈内存可以调小节约内存，默认1M。
MEM_OPTS="$MEM_OPTS -Xss256k"

#堆外内存的最大值默认约等于堆大小，可以显式将其设小，获得一个比较清晰的内存总量预估
#MEM_OPTS="$MEM_OPTS -XX:MaxDirectMemorySize=1g"

#CMSInitiatingOccupancyFraction 设置年老代空间被使用75%后出发CMS收集器
#UseCMSInitiatingOccupancyOnly 只在达到阈值后才触发CMS
#GC_OPTS="$GC_OPTS -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly"


# System.gc() 使用CMS算法
#GC_OPTS="$GC_OPTS -XX:+ExplicitGCInvokesConcurrent"

# CMS中的下列阶段并发执行
#GC_OPTS="$GC_OPTS -XX:+ParallelRefProcEnabled -XX:+CMSParallelInitialMarkEnabled"

# 根据应用的对象生命周期设定，减少事实上的老生代对象在新生代停留时间，加快YGC速度
GC_OPTS="$GC_OPTS -XX:MaxTenuringThreshold=3"

# 如果OldGen较大，加大YGC时扫描OldGen关联的卡片(每个card大小为512byte)，加快YGC速度，默认值256较低(256*512B)=128K
GC_OPTS="$GC_OPTS -XX:+UnlockDiagnosticVMOptions -XX:ParGCCardsPerStrideChunk=1024"

SOA_BASE="-Dsoa.base=$PRGDIR/../ -Dsoa.run.mode=native"

OPTIMIZE_OPTS="-XX:-UseBiasedLocking -XX:AutoBoxCacheMax=20000 -Djava.security.egd=file:/dev/./urandom"

SHOTTING_OPTS="-XX:+PrintCommandLineFlags -XX:-OmitStackTraceInFastThrow -XX:ErrorFile=${LOGDIR}/hs_err_%p.log"
SHOTTING_OPTS="$SHOTTING_OPTS -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints"

OTHER_OPTS="-Djava.net.preferIPv4Stack=true -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Dsun.jun.encoding=UTF-8"

apmEnable="$apm_enable"
if [ "$apmEnable" == "true" ]; then
    JAVA_OPTS="$E_JAVA_OPTS $NETTY_OPTS $SOA_BASE $DEBUG_OPTS $USER_OPTS $MEM_OPTS $GC_OPTS $OPTIMIZE_OPTS $SHOTTING_OPTS $JMX_OPTS $OTHER_OPTS $APM_OPTS"
else
    JAVA_OPTS="$E_JAVA_OPTS $NETTY_OPTS $SOA_BASE $DEBUG_OPTS $USER_OPTS $MEM_OPTS $GC_OPTS $OPTIMIZE_OPTS $SHOTTING_OPTS $JMX_OPTS $OTHER_OPTS"
fi

# SIGTERM-handler  graceful-shutdown
pid=0
process_exit() {
 if [ $pid -ne 0 ]; then
  kill -SIGTERM "$pid"
  wait "$pid"
 fi

for fluentPid in $(pgrep -f fluent-bit)
 do
    kill -SIGTERM "$fluentPid"
    wait "$fluentPid"
 done

 exit 143; # 128 + 15 -- SIGTERM
}


trap 'kill ${!};process_exit' SIGTERM

echo $JAVA_OPTS > $LOGDIR/console.log

nohup java -server $JAVA_OPTS -cp ./dapeng-bootstrap.jar com.github.dapeng.bootstrap.Bootstrap >> $LOGDIR/console.log 2>&1 &
pid="$!"
echo $pid > $LOGDIR/pid.txt

fluentBitEnable="$fluent_bit_enable"

if [ "$fluentBitEnable" == "" ]; then
    fluentBitEnable="false"
fi

if [ "$fluentBitEnable" == "true" ]; then
   nohup sh /opt/fluent-bit/fluent-bit.sh >> $LOGDIR/fluent-bit.log 2>&1 &
fi

wait $pid

