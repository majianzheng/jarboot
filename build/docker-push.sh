#!/bin/bash

# 项目安装路径
projectDir=$(cd `dirname $0`/../; pwd)
source $projectDir/build/common.sh || exit

NAME=jarboot
VERSION=3.0.0

docker build --rm -t ${NAME}:latest -f $projectDir/build/Dockerfile $projectDir/packaging/target/jarboot-bin/  \
--build-arg VERSION=$VERSION
