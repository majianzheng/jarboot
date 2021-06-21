#!/usr/bin/env bash
#设定jarboot-service.jar的JVM参数
#可根据服务器条件调整内存大小，本身内存占有很小不需要配置太大
JVM="-Xms256m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=500 -XX:-HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./jarboot_pid<pid>.hprof"
#启动
java -jar $JVM jarboot-service.jar
