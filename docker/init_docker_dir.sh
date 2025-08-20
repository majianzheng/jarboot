#!/bin/bash

# 项目安装路径
projectDir=$(cd `dirname $0`/../; pwd)

pushd $projectDir/docker
echo "init docker dir"
mkdir -p ./data/jarboot1 ./data/jarboot2 ./logs/jarboot1 ./logs/jarboot2 ./workspace1 ./workspace2

# 检查workspace1是否为空目录，如果是则复制文件
if [ -d "./workspace1" ] && [ -z "$(ls -A ./workspace1)" ]; then
    if [ -d "$projectDir/packaging/target/jarboot-bin/jarboot/workspace" ]; then
        echo "Copying files to workspace1..."
        cp -r $projectDir/packaging/target/jarboot-bin/jarboot/workspace/* ./workspace1/ 2>/dev/null || true
    fi
fi

# 检查workspace2是否为空目录，如果是则复制文件
if [ -d "./workspace2" ] && [ -z "$(ls -A ./workspace2)" ]; then
    if [ -d "$projectDir/packaging/target/jarboot-bin/jarboot/workspace" ]; then
        echo "Copying files to workspace2..."
        cp -r $projectDir/packaging/target/jarboot-bin/jarboot/workspace/* ./workspace2/ 2>/dev/null || true
    fi
fi

echo "change owner"
sudo chown -R 1000:1000 ./data ./logs ./workspace1 ./workspace2 ./config
echo "success!"
popd
