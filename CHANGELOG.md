## 1.0.10-SNAPSHOT (9.13, 2021)

#### FEATURES:
* 目录结构变更，jar文件放入bin文件夹中，增加插件目录plugins
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
