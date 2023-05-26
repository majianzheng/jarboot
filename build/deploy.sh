#!/bin/bash

# 项目安装路径
projectDir=$(cd `dirname $0`/../; pwd)
source $projectDir/build/common.sh || exit

pushd jarboot-api
mvn clean install deploy -P release
popd
pushd jarboot-common
mvn clean install deploy -P release
popd
pushd spring-boot-starter-jarboot
mvn clean install deploy -P release
popd

pushd jarboot-client
mvn clean install deploy -P release
popd
