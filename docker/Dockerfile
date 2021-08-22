FROM openjdk:17-jdk-alpine

EXPOSE 9899

ENV JARBOOT_VERSION 1.0.9
ARG BIN_FILE=jarboot-packaging-${JARBOOT_VERSION}-bin.zip

RUN wget https://repo1.maven.org/maven2/io/github/majianzheng/jarboot-packaging/$JARBOOT_VERSION/${BIN_FILE} && \
  unzip -o $BIN_FILE && \
  rm -f $BIN_FILE

WORKDIR jarboot

VOLUME ["conf","services","logs","data"]

CMD ["jarboot.jarboot"]

ENTRYPOINT ["java","-Ddocker=true","-Xms256m","-Xmx256m","-XX:+UnlockExperimentalVMOptions","-XX:+UseZGC","-XX:-OmitStackTraceInFastThrow","-XX:+HeapDumpOnOutOfMemoryError","-XX:HeapDumpPath=./logs/java_heapdump.hprof","-XX:-UseLargePages","-jar","jarboot-server.jar"]
LABEL jarboot.image.authors="majianzheng"
