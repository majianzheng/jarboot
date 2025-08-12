# ------------
# 定义工具函数
# ------------

# 根据指定内容查找pid
find_pid() {
  pid=$(ps aux | grep "$1" | grep -v grep | grep -v status.sh | grep -v startup.sh | grep -v shutdown.sh | awk '{print $2}')
  echo ${pid}
}

#确定端口已经起来了
wait_until_port_up() {
   port=$1
   max_try_times=$2
   if [[ "X${max_try_times}" = "X" ]]; then
        max_try_times=5
   fi
   port_exist=`netstat -nltp | grep "${port} "`
   try_times=1

    while [[ "X${port_exist}" = "X" ]] && (( ${try_times} < ${max_try_times} ))
    do
        sleep 2s
        port_exist=`netstat -nltp | grep "${port} "`
        (( try_times+=1 ))
        echo "waiting ${port} up ..."
    done

    if (( $try_times >= $max_try_times )); then
        return 1
    else
        return 0
    fi

}

# 确定服务开启以后再结束等待
# 每隔两秒会检查一次，5次后还没有就返回异常
wait_until_startup() {
  service=$1
  sleep 2s
  pid=$(find_pid ${service})
  try_times=1

  while [[ "X${pid}" = "X" ]] && (( $try_times < 5 ))
  do
    sleep 2s
    pid=$(find_pid ${service})
    (( try_times+=1 ))
  done
  if (( try_times >= 5 )); then
    return 1
  else
    return 0
  fi
}

# 确定服务关闭以后再结束等待
# 每隔两秒会检查一次，5次后还没有就kill -9
wait_until_shutdown() {
  service=$1
  pid=$(find_pid ${service})
  try_times=1

  while [[ "X${pid}" != "X" ]] && (( $try_times < 5 ))
  do
    sleep 2s
    pid=$(find_pid ${service})
    (( try_times+=1 ))
  done
  if (( try_times >= 5 )); then
    return 1
  else
    return 0
  fi
}

# 根据进程关键字关闭进程
kill_process() {
  service=$1
  pid=$(find_pid ${service})
  if [[ "X${pid}" = "X" ]]; then
    return 0
  fi
  kill ${pid}
  wait_until_shutdown ${service}
  if [[ $? != 0 ]]; then
    kill -9 ${pid}
  fi
}


# 根据进程关键字关闭进程
kill_process_sudo() {
  service=$1
  pid=$(find_pid ${service})
  if [[ "X${pid}" = "X" ]]; then
    return 0
  fi
  kill ${pid}
  wait_until_shutdown ${service}
  if [[ $? != 0 ]]; then
    kill -9 ${pid}
  fi
}

# 如果上一条命令执行失败，则退出程序
exit_if_error() {
  success=$?
  message=$1
  if [[ ${success} != 0 ]]; then
    if [[ "X${message}" != "X" ]]; then
      echo ${message}
    fi
    exit 1
  fi
}

wait_exit() {
  service=$1
  pid=$(find_pid ${service})
  try_times=1

  while [[ "X${pid}" != "X" ]] && (( $try_times < 5 ))
  do
    sleep 15s
    pid=$(find_pid "${service}")
  done
  return 1;
}