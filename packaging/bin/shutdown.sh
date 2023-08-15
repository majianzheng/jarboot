#!/bin/bash

cd `dirname $0`

pid=`ps ax | grep -i 'jarboot.daemon' | grep java | grep -v grep | awk '{print $1}'`
if [ -z "$pid" ] ; then
  echo "No jarboot daemon running."
else
  echo "Kill jarboot daemon ${pid}..."
  kill ${pid}
fi

pid=`ps ax | grep -i 'jarboot.jarboot' | grep java | grep -v grep | awk '{print $1}'`
if [ -z "$pid" ] ; then
        echo "No jarboot server running."
        exit -1;
fi

echo "The jarboot server(${pid}) is running..."

kill ${pid}

echo "Send shutdown request to jarboot server(${pid}) OK"
