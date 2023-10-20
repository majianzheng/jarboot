#!/bin/bash

cygwin=false
darwin=false
os400=false
case "`uname`" in
CYGWIN*) cygwin=true;;
Darwin*) darwin=true;;
OS400*) os400=true;;
esac
error_exit ()
{
    echo "ERROR: $1 !!"
    exit 1
}
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=$HOME/jdk/java
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=/usr/java
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=/opt/taobao/java
[ ! -e "$JAVA_HOME/bin/java" ] && unset JAVA_HOME

if [ -z "$JAVA_HOME" ]; then
  if $darwin; then

    if [ -x '/usr/libexec/java_home' ] ; then
      export JAVA_HOME=`/usr/libexec/java_home`

    elif [ -d "/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home" ]; then
      export JAVA_HOME="/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home"
    fi
  else
    JAVA_PATH=`dirname $(readlink -f $(which javac))`
    if [ "x$JAVA_PATH" != "x" ]; then
      export JAVA_HOME=`dirname $JAVA_PATH 2>/dev/null`
    fi
  fi
  if [ -z "$JAVA_HOME" ]; then
        error_exit "Please set the JAVA_HOME variable in your environment, We need java(x64)! jdk8 or later is better!"
  fi
fi

export SERVER="jarboot-server"
CUR_DIR=`pwd`
export JAVA_HOME
export JAVA="$JAVA_HOME/bin/java"
export JARBOOT_HOME=$(cd `dirname $0`/../; pwd)
source $JARBOOT_HOME/bin/common.sh || exit
cd "${JARBOOT_HOME}"
#===========================================================================================
# JVM Configuration
#===========================================================================================
JAVA_OPT="${JAVA_OPT} -Xms512m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=500"
JAVA_OPT="${JAVA_OPT} -XX:-OmitStackTraceInFastThrow -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=logs/java_heapdump.hprof"
JAVA_OPT="${JAVA_OPT} -XX:-UseLargePages"

JAVA_MAJOR_VERSION=$($JAVA -version 2>&1 | sed -E -n 's/.* version "([0-9]*).*$/\1/p')
if [[ "$JAVA_MAJOR_VERSION" -ge "9" ]] ; then
  JAVA_OPT="${JAVA_OPT} -Xlog:gc*:file=logs/jarboot_gc.log:time,tags:filecount=10,filesize=102400"
else
  JAVA_OPT="${JAVA_OPT} -Djava.ext.dirs=${JAVA_HOME}/jre/lib/ext:${JAVA_HOME}/lib/ext"
  JAVA_OPT="${JAVA_OPT} -Xloggc:logs/jarboot_gc.log -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=100M"
fi
JAVA_OPT="${JAVA_OPT} -Djdk.attach.allowAttachSelf=true -Dloader.path=components/lib,plugins/server -Dfile.encoding=UTF-8"

JAR_FILE=components/${SERVER}.jar

JAVA_OPT="${JAVA_OPT} -jar ${JAR_FILE}"

if [ ! -d "${JARBOOT_HOME}/logs" ]; then
  mkdir "${JARBOOT_HOME}/logs"
fi

echo "$JAVA ${JAVA_OPT}"

echo "jarboot is starting"

# start
echo "$JAVA ${JAVA_OPT}"
nohup $JAVA ${JAVA_OPT} jarboot.jarboot >/dev/null &
echo "jarboot is starting，you can check the ${JARBOOT_HOME}/logs/jarboot.log"
echo "Starting jarboot server daemon..."
TOOL_JAR="${JARBOOT_HOME}/components/jarboot-tools.jar io.github.majianzheng.jarboot.tools.daemon.ServerDaemon"
DAEMON_VM="-Xms10m -Xmx10m -XX:+UseG1GC -XX:MaxGCPauseMillis=500 -DJARBOOT_HOME=$JARBOOT_HOME"
nohup $JAVA $DAEMON_VM -cp ${TOOL_JAR} jarboot.daemon >/dev/null &
echo "daemon started."