FROM anolis-registry.cn-zhangjiakou.cr.aliyuncs.com/openanolis/openjdk:17-8.6

# 设置时区为东八区 (上海)
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \

EXPOSE 9899

ARG VERSION

ENV JARBOOT_DOCKER true
ENV VERSION $VERSION

COPY jarboot /jarboot

WORKDIR jarboot

ENV JARBOOT_HOME /jarboot

VOLUME ["/jarboot/conf","/jarboot/workspace","/jarboot/logs","/jarboot/data", "/jarboot/plugins"]

CMD ["jarboot.jarboot"]


ENTRYPOINT ["java","-Ddocker=true", "-Djdk.attach.allowAttachSelf=true", "-XX:+EnableDynamicAgentLoading","-Xms256m","-Xmx256m","-XX:+UseG1GC","-XX:-OmitStackTraceInFastThrow","-XX:+HeapDumpOnOutOfMemoryError","-XX:HeapDumpPath=logs/java_heapdump.hprof","-XX:-UseLargePages", "-Dloader.path=components/lib,plugins/server","-jar","components/jarboot-server.jar"]
LABEL jarboot.image.authors="majianzheng"
