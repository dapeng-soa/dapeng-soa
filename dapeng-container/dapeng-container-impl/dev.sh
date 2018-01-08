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

# soa-parent
soaparentdir=${basedir}/../../
cd ${soaparentdir}
mvn clean install

# container
containerdir=${basedir}
cd ${containerdir}
mvn clean package -Pdev

# engine
enginedir=${basedir}/../dapeng-bootstrap
cd ${enginedir}
mvn clean package -Pdev

cp ${enginedir}/target/dapeng-bootstrap*.jar ${containerdir}/target/dapeng-container/bin/
cp -rf ${soaparentdir}/dapeng-message/dapeng-message-kafka/target/dapeng-message-kafka ${containerdir}/target/dapeng-container/plugin/