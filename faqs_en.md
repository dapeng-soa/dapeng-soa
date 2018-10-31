## 1.get ip error when container boots up
```
Caused by: java.lang.RuntimeException: wrong with get ip
              at: com.github.dapeng.core.helper.IPUtils.<init>(IPUtils.java:23)
```

Always occurs within Linux。

Solution：Bind hostname and ip address in /etc/hosts
Take centos for example：
>echo $(ifconfig eth0|grep "inet "|awk '{print $2}') `hostname` >> /etc/hosts
