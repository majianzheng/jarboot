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

TOOL_JAR="${JARBOOT_HOME}/components/jarboot-tools.jar io.github.majianzheng.jarboot.tools.shell.GenClusterSecretKey"
DAEMON_VM="-Xms10m -Xmx10m -XX:+UseG1GC -XX:MaxGCPauseMillis=500 -DJARBOOT_HOME=$JARBOOT_HOME"

$JAVA $DAEMON_VM -cp ${TOOL_JAR} jarboot.status
echo "Done."
