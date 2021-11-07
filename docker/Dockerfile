FROM openjdk:17-jdk-alpine

EXPOSE 9899

ENV JARBOOT_VERSION 1.1.1

COPY jarboot /jarboot

WORKDIR jarboot

ENV JARBOOT_HOME /jarboot

VOLUME ["/jarboot/conf","/jarboot/services","/jarboot/logs","/jarboot/data"]

CMD ["jarboot.jarboot"]


ENTRYPOINT ["java","-Ddocker=true","-Xms256m","-Xmx256m","-XX:+UnlockExperimentalVMOptions","-XX:+UseZGC","-XX:-OmitStackTraceInFastThrow","-XX:+HeapDumpOnOutOfMemoryError","-XX:HeapDumpPath=./logs/java_heapdump.hprof","-XX:-UseLargePages", "-Dloader.path=./plugins/server","-jar","bin/jarboot-server.jar"]
LABEL jarboot.image.authors="majianzheng"
