#!/bin/bash

cd `dirname $0`
target_dir=`pwd`

pid=`ps ax | grep -i 'jarboot.jarboot' | grep ${target_dir} | grep java | grep -v grep | awk '{print $1}'`
if [ -z "$pid" ] ; then
        echo "No jarboot server running."
        exit -1;
fi

echo "The jarboot server(${pid}) is running..."

kill ${pid}

echo "Send shutdown request to jarboot server(${pid}) OK"
