#!/bin/bash

# 项目安装路径
projectDir=$(cd `dirname $0`/../; pwd)
source $projectDir/build/common.sh || exit

mvn clean install -P prod
