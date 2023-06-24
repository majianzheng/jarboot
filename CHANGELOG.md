## 3.0.0-beta (6, 2023)

- 前端使用Vue3重构，支持暗黑主题，支持主题跟随系统变化
- 不再局限于只支持Java程序，支持启动Shell脚本、二进制等程序
- 用户域隔离，每个账号可使用独立的目录
- 新增工具菜单：文件浏览器、终端(需要JDK11或以上版本)、文本及代码编辑
- dashboard界面使用图表，可记录历史趋势


## 2.3.1 (2, 2022)
### 接口改动较大，旧框架代码移至【2.2.x】分支
- 命令执行通讯协议改为二进制传输
- 使用新开发的事件框架重构后端消息流
- sonar lint和pmd修改
#### FEATURES:
- 增加针对开发者的API的client模块实现
- 增加std输出重定向到文件的支持，使用VM参数jarboot.stdout.file和jarboot.stdout.file.always指定文件
- Linux或macOS中使用nohup启动服务

## 2.2.4 (1, 2022)

- 修复spring.bean命令构建错误问题
- 修复偶尔出现的重复启动的问题
- 修复detach远程进程时仍然弹出是否信任的问题
- 修复在线诊断，引入spring-boot-starter-jarboot后无法执行spring扩展命令的问题
- 修复服务管理搜索名字显示异常的问题
- 修复偶尔会出现的线程池忙碌问题
- 远程进程detach时增加确认提示
#### FEATURES:
- 设置界面增加子菜单，信任服务器增删和查看界面
- Console支持是否自动换行、是否自动滚动到底部
- 移除列表显示模式

## 2.2.3 (12.28, 2021)

- spring-boot全家桶升级2.6.2版本（logback v1.2.9）
- 安全性增强，部分开放接口增加token认证
- 远程进程诊断时，增加安全认证，点击受信任后才可以诊断
- 日志收集系统，分布式统一集中记录
- 修复使用反向代理时每隔一段时间重连一次的问题
- 修复断开重连时有时未实时推送服务状态更新的问题
  
## 2.2.2 (12.20, 2021)

- fix: #29 jarboot用nginx发布后，首页加载js和css错误无法打开页面。
  注意：Nginx除了普通HTTP外，还需要配置Websocket代理
- 安装目录全路径中存在空白字符时报错并退出
- 服务管理排除含有空白字符的名称
- 后端代码性能优化，可读性优化，增加注释
#### FEATURES:
- 增加jt.sh、jt.cmd脚本，可以快捷的Attach和启动Java进程
- 终端ANSI标准支持——炫彩终端
- 新增隐藏命令shutdown/close，可用与断开诊断进程并重置增强类以及清理资源
- 服务管理双击行时启动服务
- 在线调试更名为在线诊断
- 在线诊断界面改版，不同服务器的进程分组显示
- 在线诊断本地进程双击行时Attach对应的进程

## 2.2.1 (12.07, 2021)

- 修复工作空间变更后工作空间文件监控功能还是旧目录的问题
- 大幅度优化std的IO性能，重构缓存刷新机制，实现空闲期 0 CPU占用
- dashboard、jad、heapdump命令的渲染界面优化，交互设计改进
- 内存优化，占用更少的内存空间
- 修复Attach本地进程时未初始化而不显示控制台输出的问题
- 优化工作空间文件监控逻辑，原file-record.temp文件废弃，可删除
- 修复derby日志文件在根目录的问题，移到logs目录，原derby.log文件可删除
- 优化后端线程的调度管理
- 修复删除服务时，文件太多无响应的问题，增加全局loading提示
- 修复导入服务时，压缩文件内容过多时无响应的问题，增加全局loading提示
- 代码优化，可读性优化，完善代码注释
- 远程进程连接网络断开时，增加心跳及尝试重连机制，每隔一段时间探测一次
- 优化命令执行的通讯协议
- 修复notice接口指定sessionId时仍通知所以客户端的问题，优化notice的前后端交互机制
- 优化前端布局，权限控制、设置、帮助使用左侧固定右侧自适应布局方式

## 2.2.0 (11.30, 2021)

- 修复cat命令读取xml、html文件时没有显示真实内容的问题
- 服务管理，树显示时默认显示第一个节点的第一个孩子
#### FEATURES:
- 服务管理，增加导入、导出功能
- 上传服务文件开始前，提示是否备份，若备份则导出当前的服务文件夹快照
- 恢复thread命令显示内部线程信息的功能

## 2.1.0 (11.25, 2021)

- 修复在Docker下启动多个容器时，将logs目录挂载后出现的状态不对的问题
- 修复Safari浏览器滚动条下部有一个小白点的问题
- 隐藏火狐浏览器滚动条
- 前端样式代码优化统一
- 在线调试增加正在Attach的图标过渡
#### FEATURES:
- 图标更新美化
- 支持以客户端的模式与k8s、Docker集成使用，集中管理、诊断

## 2.0.0 (11.22, 2021)

- 服务状态通知机制优化
- 服务配置界面增加服务状态显示
- 修复服务数量过多时服务配置界面显示异常问题
#### FEATURES:
- 界面大改版，菜单结构大调整
- 主题色修改，紧凑版布局
- 服务配置新增名字和组的配置，可以重命名和配置所属组
- 服务管理增加底部工具栏，一键启动等按钮、树显示VS列表显示、控制台VS服务配置
- 服务管理新增组显示，可与列表显示相互切换
- 服务管理侧栏固定式布局
- 部分图标更新、美化
- 服务配置由菜单移至服务管理，服务管理右侧界面可以在控制台和服务配置之间相互切换
- 插件管理移到设置中
- 统一滚动条样式

## 1.1.3 (11.17, 2021)

- 关于界面调整，菜单名改为帮助，关于为其子菜单
- 设置界面调整，显示美化
- 上传服务文件的上传控件标题错误的问题修改
- 内部代码优化

## 1.1.2 (11.12, 2021)

- 修复VM options编辑界面偶尔出现的显示异常问题
- 修复JDK 11及以上版本，在线调试无法Attach自己的问题
- 修复当服务列表为空或者未选中服务时，点击上传按钮界面崩掉的问题
- 修复提交服务配置时，400错误的问题

## 1.1.1 (11.7, 2021)

- 修复清空Console终端时会显示loading的动态的问题
- <code>关于</code>界面简化更新
#### FEATURES:
- 在线调试功能，可显示并Attach调试当前服务器上除自己以外的所有运行的Java进程
- 按钮使能，可直观的展示按钮当前是否可以使用
- 新增服务上传界面优化更新
- 服务管理增加删除工具按钮

## 1.1.0 (11.3, 2021)

* 服务配置："是否可执行jar"、"启动的jar文件"和"自定义的命令"这3项配置合为一个"启动命令"的配置项，
  若为空且仅有一个jar文件则默认使用-jar选项启动，旧版本配置内容将失效
* 使用重新设计的进程识别机制，解决无法启动<code>seata</code>这种带传入参数的Java程序
* 修复启动服务时Console终端未清理bug
* shell和debug插件优化修改
* thread命令不再显示内部线程信息，兼容jdk11及以上的环境编译
* 重连成功和工作空间变化时自动刷新服务列表
#### FEATURES:
* 前端界面框架升级优化
* 服务管理界面增加按名称、状态搜索过滤的功能
* 服务配置界面增加搜索过滤功能

## 1.0.10 (10.24, 2021)

* 配置文件修改jarboot.services.root-dir -> jarboot.services.workspace
* 目录结构变更，jar文件放入bin文件夹中，增加插件目录plugins
#### FEATURES:
* Console控制台支持print和退格
* 支持数据库驱动放入plugins/server下以支持更多数据库
* 支持插件式开发扩展，agent类型插件可扩充命令，server类型插件可增强服务端功能
* 命令输入框支持历史记录上下翻页，快速输入历史命令
* docker支持，识别是否在docker中运行，在docker中运行时示例程序没有界面
* 增加自定义启动参数配置，不局限于可执行的jar文件，可以自定义执行字节码文件（***.class），可以使用classpath和-cp指定执行类
* 示例程序增加2个SPI自定义命令pow和fib，在docker中可以通过开启两个浏览器界面同时测试多个调试命令

## 1.0.9 (8.14, 2021)

* refactor bytes command (重构查看字节码命令)
* fix upload file and download file 401 error
* task filter by workspace
* fix request error when token expire
* fix command cli parse not use default when not require
#### FEATURES:
* add dump command

## 1.0.8 (8.7, 2021)

* refactor command protocol (重构命令执行协议)
* stdout default on, and change from session to broadcast (stdout命令默认开启，改为广播级，将广播到所有客户端)
* 启动完成判定时间改为由VM参数传入，原配置文件中的该项配置废弃
* 重构消息交互机制，优化性能
#### FEATURES:
* Agent api and command SPI （增加api主动通知启动完成接口，自定义命令SPI扩展，支持用户自己开发命令）
* When import <code>spring-boot-starter-jarboot</code>, spring.env and spring.bean command added
* add help command
* add ognl command
* add sm command
* add sysenv command
* add tt command
* add stack command
* add pwd command

## 1.0.7 (7.25, 2021)

* [#13] Command line parse error when space in quotation
* Rename module name from jarboot-service to jarboot-server
* Show the current version when start
* Fix priority sorted error
* Refactor modify some url api, service boot properties
* fastjson -> jackson
* code format add p3c, sonar, dependency and findbugs plugins
* Global config move in jarboot.properties
#### FEATURES:
* Support jar file, working directory and jdk path using absolute path or relative path.
* jarboot.properties add jarboot.services.enable-auto-start-after-start config.
* Server vm option from a file, and can edit in ui.
* Server start main arguments edit ui.
* Architecture optimization to support more concurrent terminal messages.
* Add startup.sh startup.cmd shutdown.sh shutdown.cmd file to start or shutdown jarboot server.
* Services sorted by name.
* Remove swagger-ui

## 1.0.6 (7.11, 2021)

#### FEATURES:

* User manager and permission control.
