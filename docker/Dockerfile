FROM openjdk:12-jdk

EXPOSE 9899

ENV JARBOOT_DOCKER true

COPY jarboot /jarboot

WORKDIR jarboot

ENV JARBOOT_HOME /jarboot

VOLUME ["/jarboot/conf","/jarboot/services","/jarboot/logs","/jarboot/data"]

CMD ["jarboot.jarboot"]


ENTRYPOINT ["java","-Ddocker=true", "-Djdk.attach.allowAttachSelf=true","-Xms256m","-Xmx256m","-XX:+UseG1GC","-XX:-OmitStackTraceInFastThrow","-XX:+HeapDumpOnOutOfMemoryError","-XX:HeapDumpPath=logs/java_heapdump.hprof","-XX:-UseLargePages", "-Dloader.path=plugins/server","-jar","bin/jarboot-server.jar"]
LABEL jarboot.image.authors="majianzheng"
