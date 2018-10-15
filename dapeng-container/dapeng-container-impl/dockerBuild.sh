#!/usr/bin/env bash

#脚本目录
basedir=`pwd`
dirname $0|grep "^/" >/dev/null
if [ $? -eq 0 ];then
   basedir=`dirname $0`
else
    dirname $0|grep "^\." >/dev/null
    retval=$?
    if [ $retval -eq 0 ];then
        basedir=`dirname $0|sed "s#^.#$basedir#"`
    else
        basedir=`dirname $0|sed "s#^#$basedir/#"`
    fi
fi

# 执行打包
sh dev.sh

# 构建镜像
containerdir=${basedir}
cd ${containerdir}
mvn package -PdockerBuild