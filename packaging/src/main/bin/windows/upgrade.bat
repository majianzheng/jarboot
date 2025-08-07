@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set sourceDir=%~dp0
set sourceDir=%sourceDir:~0,-13%

set "installDir="
set "autoConfirm=false"

:: 解析命令行参数
:parse_args
if "%1"=="" goto :check_install_dir
if "%1"=="-d" (
    set "installDir=%2"
    shift
    shift
    goto :parse_args
)
if "%1"=="-y" (
    set "autoConfirm=true"
    shift
    goto :parse_args
)
shift
goto :parse_args

:check_install_dir
@rem 检查是否提供了安装目录
if "!installDir!"=="" (
    echo 未指定安装目录，请输入安装目录路径:
    set /p "installDir="
)

@rem 检查安装目录是否存在
if "!installDir!"=="" (
    echo 错误: 未输入安装目录路径
    pause
    exit /b 1
)

if not exist "!installDir!" (
    echo 错误: 指定的安装目录不存在: !installDir!
    pause
    exit /b 1
)

echo Install directory: !installDir!

@rem 步骤1: 提示是否继续（将关闭系统）
if "!autoConfirm!"=="false" (
    echo 警告：升级将关闭系统，是否继续 [Y/N]
    set /p "confirm="
    if /i not "!confirm!"=="Y" (
        echo 升级已取消
        pause
        exit /b 0
    )
) else (
    echo 自动确认模式：正在关闭系统...
)

@rem 调用安装目录下的shutdown.cmd脚本关闭软件
set "shutdownScript=!installDir!\bin\windows\shutdown.cmd"
if exist "!shutdownScript!" (
    echo 正在关闭系统...
    echo. | call "!shutdownScript!"
) else (
    echo 警告: shutdown.cmd 脚本未找到: !shutdownScript!
)

@rem 等待一段时间确保系统完全关闭
timeout /t 3 /nobreak >nul

@rem 步骤2 删除安装目录下的components、fe目录
echo 正在删除旧文件...
if exist "!installDir!\components" (
    rd /s /q "!installDir!\components"
)
if exist "!installDir!\fe" (
    rd /s /q "!installDir!\fe"
)


echo 复制新文件...

@rem 拷贝components目录
if exist "%sourceDir%\components" (
    xcopy "%sourceDir%\components" "!installDir!\components" /E /I /H /Y >nul
)

@rem 拷贝fe目录
if exist "%sourceDir%\fe" (
    xcopy "%sourceDir%\fe" "!installDir!\fe" /E /I /H /Y >nul
)

@rem 拷贝plugins目录
if exist "%sourceDir%\plugins" (
    xcopy "%sourceDir%\plugins" "!installDir!\plugins" /E /I /H /Y >nul
)

@rem 拷贝bin目录
if exist "%sourceDir%\bin" (
    xcopy "%sourceDir%\bin" "!installDir!\bin" /E /I /H /Y >nul
)

@rem 拷贝conf、data、script、workspace目录（如果安装目录中不存在则拷贝，若存在则忽略）
if exist "%sourceDir%\conf" (
    if not exist "!installDir!\conf" (
        xcopy "%sourceDir%\conf" "!installDir!\conf" /E /I /H /Y >nul
    )
)

if exist "%sourceDir%\data" (
    if not exist "!installDir!\data" (
        xcopy "%sourceDir%\data" "!installDir!\data" /E /I /H /Y >nul
    )
)

if exist "%sourceDir%\script" (
    if not exist "!installDir!\script" (
        xcopy "%sourceDir%\script" "!installDir!\script" /E /I /H /Y >nul
    )
)

if exist "%sourceDir%\workspace" (
    if not exist "!installDir!\workspace" (
        xcopy "%sourceDir%\workspace" "!installDir!\workspace" /E /I /H /Y >nul
    )
)

@rem 步骤4 提示升级完成，询问是否启动系统
set "startSystem=false"
if "!autoConfirm!"=="false" (
    echo 升级完成！是否立即启动系统 [Y/n]
    set /p "startConfirm="

    @rem 默认选是
    if "!startConfirm!"=="" set "startConfirm=Y"
    if /i "!startConfirm!"=="Y" (
        set "startSystem=true"
    ) else (
        set "startSystem=false"
    )
) else (
    echo 自动确认模式：正在启动系统...
    set "startSystem=true"
)

if "!startSystem!"=="true" (
    set "startupScript=!installDir!\bin\windows\startup.cmd"
    if exist "!startupScript!" (
        echo 正在启动系统...
        echo. | call "!startupScript!"
    ) else (
        echo 错误: startup.cmd 脚本未找到: !startupScript!
    )
) else (
    echo 升级已完成，系统未启动。你可以手动执行 !installDir!\bin\windows\startup.cmd 启动系统
)

echo 升级执行完毕
if "!autoConfirm!"=="false" (
    pause
)
