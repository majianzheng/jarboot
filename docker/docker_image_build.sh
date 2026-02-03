#!/bin/bash

# 项目安装路径
projectDir=$(cd `dirname $0`/../; pwd)

NAME=jarboot
VERSION=3.3.0

docker rmi ${NAME}:latest

docker build --rm -t ${NAME}:latest -f $projectDir/docker/Dockerfile $projectDir/packaging/target/jarboot-bin/  \
--build-arg VERSION=$VERSION
