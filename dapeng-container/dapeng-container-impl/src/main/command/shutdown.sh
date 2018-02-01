#!/usr/bin/env bash

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

cat $workdir/../logs/pid.txt | while read line
do
  kill $line
  echo ...is killing pid $line...
  while true;
  do
    pro=$(ps $line | grep $line | wc -l)
    echo '...please wait...'
    if [ $pro -ge 1 ]
      then
        sleep 1
      else break
    fi
  done
done

echo '...shutdown finish....'

rm -rf $workdir/../logs/pid.txt

touch $workdir/../logs/pid.txt