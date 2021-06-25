@echo off
java -jar -Xms256m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=500 -XX:-HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="./jarboot_pid<pid>.hprof" jarboot-service.jar
