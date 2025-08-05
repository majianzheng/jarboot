#!/bin/bash

# 获取脚本所在目录并定位sourceDir
sourceDir=$(cd `dirname $0`/../; pwd)

# 初始化变量
installDir=""

# 解析命令行参数
while getopts "d:" opt; do
  case $opt in
    d)
      installDir=$OPTARG
      ;;
    \?)
      echo "无效选项: -$OPTARG" >&2
      exit 1
      ;;
  esac
done

# 检查是否提供了安装目录
if [ -z "$installDir" ]; then
  echo "未指定安装目录，请输入安装目录路径:"
  read -r installDir

  # 检查用户输入是否为空
  if [ -z "$installDir" ]; then
    echo "错误: 未输入安装目录路径"
    exit 1
  fi
fi

# 检查安装目录是否存在
if [ ! -d "$installDir" ]; then
  echo "指定的安装目录不存在: $installDir"
  exit 1
fi

# 步骤1: 提示是否继续（将关闭系统）
echo "警告：升级将关闭系统，是否继续？(Y/N)"
read -r confirm
if [ "$confirm" != "Y" ] && [ "$confirm" != "y" ]; then
  echo "升级已取消"
  exit 0
fi

# 调用安装目录下的shutdown.sh脚本关闭软件
shutdownScript="$installDir/bin/shutdown.sh"
if [ -f "$shutdownScript" ]; then
  echo "正在关闭系统..."
  bash "$shutdownScript"
else
  echo "警告: shutdown.sh 脚本未找到: $shutdownScript"
fi

# 等待一段时间确保系统完全关闭
sleep 3

# 步骤2: 删除安装目录下的components、fe目录
echo "正在删除旧文件..."
[ -d "$installDir/components" ] && rm -rf "$installDir/components"
[ -d "$installDir/fe" ] && rm -rf "$installDir/fe"

# 步骤3: 拷贝新文件
echo "正在拷贝新文件..."
# 拷贝components目录
if [ -d "$sourceDir/components" ]; then
  cp -rf "$sourceDir/components" "$installDir/"
fi

# 拷贝fe目录
if [ -d "$sourceDir/fe" ]; then
  cp -rf "$sourceDir/fe" "$installDir/"
fi

# 拷贝plugins目录
if [ -d "$sourceDir/plugins" ]; then
  cp -rf "$sourceDir/plugins" "$installDir/"
fi

# 拷贝bin目录
if [ -d "$sourceDir/bin" ]; then
  cp -rf "$sourceDir/bin" "$installDir/"
fi

# 拷贝conf、data、script、workspace目录（如果安装目录中不存在则拷贝，若存在则忽略）
if [ -d "$sourceDir/conf" ] && [ ! -d "$installDir/conf" ]; then
  cp -rf "$sourceDir/conf" "$installDir/"
fi

if [ -d "$sourceDir/data" ] && [ ! -d "$installDir/data" ]; then
  cp -rf "$sourceDir/data" "$installDir/"
fi

if [ -d "$sourceDir/script" ] && [ ! -d "$installDir/script" ]; then
  cp -rf "$sourceDir/script" "$installDir/"
fi

if [ -d "$sourceDir/workspace" ] && [ ! -d "$installDir/workspace" ]; then
  cp -rf "$sourceDir/workspace" "$installDir/"
fi

# 步骤4: 提示升级完成，询问是否启动系统
echo "升级完成！是否立即启动系统？(Y/n)"
read -r startConfirm

# 默认选是
if [ -z "$startConfirm" ] || [ "$startConfirm" = "Y" ] || [ "$startConfirm" = "y" ]; then
  startupScript="$installDir/bin/startup.sh"
  if [ -f "$startupScript" ]; then
    echo "正在启动系统..."
    bash "$startupScript"
  else
    echo "错误: startup.sh 脚本未找到: $startupScript"
  fi
else
  echo "升级已完成，系统未启动。你可以手动执行 $installDir/bin/startup.sh 启动系统"
fi

echo "升级执行完毕"
