#!/usr/bin/env sh

#
# 发布脚本
# @author craneding
# @date 2016年02月01日13:00:00
# @description Copyright (c) 2015, github.com/dapeng-soa All Rights Reserved.
#

workdir=`pwd`
dirname $0|grep "^/" >/dev/null
if [ $? -eq 0 ];then
   workdir=`dirname $0`
else
    dirname $0|grep "^\." >/dev/null
    retval=$?
    if [ $retval -eq 0 ];then
        workdir=`dirname $0|sed "s#^.#$workdir#"`
    else
        workdir=`dirname $0|sed "s#^#$workdir/#"`
    fi
fi

cd $workdir


# config log dir
logdir=$workdir/../logs
if [ ! -d "$logdir" ]; then
	mkdir "$logdir"
fi

# config java home
# export JAVA_HOME=""
# export PATH="$JAVA_HOME/bin:$PATH"

# env option(priority than vm option)
# soa_container_port (default: 9090)
# soa_zookeeper_host (default: 127.0.0.1:2181)
# soa_monitor_enable (default: false)
# soa_container_usethreadpool (default: true)
# soa_core_pool_size (default: Runtime.getRuntime().availableProcessors() * 2)
# soa_remoting_mode (default: remote [remote/local])
# soa_max_read_buffer_size (defalut: 1024 * 1024 * 5)
# soa_local_host_name (default: null)

# vm option
# soa.container.port (default: 9090)
# soa.zookeeper.host (default: 127.0.0.1:2181)
# soa.monitor.enable (default: false)
# soa.container.usethreadpool (default: true)
# soa.core.pool.size (default: Runtime.getRuntime().availableProcessors() * 2)
# soa.remoting.mode (default: remote [remote/local])
# soa.max.read.buffer.size (defalut: 1024 * 1024 * 5)
# soa.local.host.name (default: null)

# netty option
# default : one thread cache is about 1MB
# io.netty.allocator.tinyCacheSize (default: 512)
# io.netty.allocator.smallCacheSize (default: 256)
# io.netty.allocator.normalCacheSize (default: 64)
# io.netty.allocator.maxCachedBufferCapacity (default: 32768)

# JVM_OPTS=""
# DEBUG_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5000"
# USER_OPTS="-Dsoa.container.port=9090 -Dsoa.zookeeper.host=127.0.0.1:2181 -Dio.netty.leakDetectionLevel=advanced -XX:MaxDirectMemorySize=128M -Dsoa.monitor.enable=false -Dsoa.core.pool.size=100"

JVM_OPTS="-Xms256m -Xmx256m -Xloggc:$logdir/gc.log -XX:+PrintGCDateStamps -XX:+PrintGCDetails -XX:+PrintGC -XX:+HeapDumpOnOutOfMemoryError"
DEBUG_OPTS=""
SOA_BASE="-Dsoa.base=$workdir/../ -Dsoa.run.mode=native -Dsoa.transactional.enable=true -Dsoa.monitor.enable=true -Dsoa.core.pool.size=100"
USER_OPTS=""

nohup java $JVM_OPTS $SOA_BASE $DEBUG_OPTS $USER_OPTS -cp ./dapeng-bootstrap.jar com.github.dapeng.bootstrap.Bootstrap >> $logdir/catalina.out 2>&1 &
echo $! > $logdir/pid.txt