## 1.容器启动时报IP获取错误
```
Caused by: java.lang.RuntimeException: wrong with get ip
              at: com.github.dapeng.core.helper.IPUtils.<init>(IPUtils.java:23)
```

一般出现在linux环境。

解决方案：在/etc/hosts中绑定本地内网ip跟机器名
以centos为例：
>echo $(ifconfig eth0|grep "inet "|awk '{print $2}') `hostname` >> /etc/hosts
