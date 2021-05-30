# Jarboot ❤️

![logo](https://gitee.com/majz0908/jarboot/raw/master/doc/jarboot.png)

[![Build Status](https://travis-ci.com/majianzheng/jarboot.svg?branch=master)](https://travis-ci.com/majianzheng/jarboot)
[![codecov](https://codecov.io/gh/majianzheng/jarboot/branch/master/graph/badge.svg?token=FP7EPSFH4E)](https://codecov.io/gh/majianzheng/jarboot)

<code>Jarboot</code> 是一个管理、监控及调试一系列Java进程的工具

在测试环境、每日构建的集成环境，可以把一系列编译输出等jar文件放入约定的目录，由<code>Jarboot</code>提供友好的浏览器ui界面和<code>http</code>接口，统一管理它的启动、停止及状态的监控，以及执行命令以获取目标服务进程的更多信息。界面同时集成了<code>Arthas</code><sup id="a1">[[1]](#f1)</sup> 调试工具，支持通过<code>Jarboot</code>的界面对目标进程调试。

## 技术背景及目标
<code>Jarboot</code> 使用<code>Java Agent</code>和<code>ASM</code>技术往目标Java进程注入代码，无业务侵入性，注入的代码仅用于和<code>Jarboot</code> 的服务实现命令交互，部分命令会修改类的字节码用于类增强，加入了与<code>Arthas</code>类似的命令系统，如获取JVM信息、监控线程状态、获取线程栈信息等。但它的功能定位与<code>Arthas</code>不同，<code>Jarboot</code> 更偏向于面向开发、测试、每日构建等。

- 🌈   浏览器界面管理，一键启、停服务
- 🔥   支持启动、停止优先级配置<sup id="a2">[[2]](#f2)</sup>
- ⭐️支持进程守护，开启后若服务异常退出则自动启动并通知
- ☀️支持文件更新监控，开启后若jar文件更新则自动重启<sup id="a3">[[3]](#f3)</sup>
- ❤️调试、查看进程信息（已实现通过Arthas调试，在【全局配置】界面里配置Arthas路径或设置<code>ARTHAS_HOME</code>环境变量）
- 🚀   调试命令执行（开发中）

采用<code>前后端分离</code>架构，前端界面采用<code>React</code>技术，脚手架使用<code>Umi</code>，组件库使用Umi内置等<code>antd</code>。后端服务主要由<code>SpringBoot</code>实现，提供http接口和静态资源代理。通过<code>WebSocket</code>向前端界面实时推送进程信息，同时与启动的Java进程维持一个长连接，以监控其状态。

模块|描述
:-|:-
jarboot-common|公共工具类实现
jarboot-agent|agent的jar启动或运行中注入目标进程
jarboot-core|在目标进程中执行的代码，使用<code>Netty</code>与主控服务交互
jarboot-service|主控服务：核心业务逻辑实现，提供http和WebSocket
jarboot-ui|前端ui界面，使用<code>React</code>实现

## 使用方法
1. 编译java和前端项目
```
//切换到代码根目录，编译Java代码
mvn clean install

cd jarboot-ui

//首次时需要先安装依赖，执行yarn或npm install
yarn

//执行编译，yarn buld或npm run build，开发模式可执行yarn start或npm run start
yarn build
```

2. 约定的目录结构，编译的输出放入如下的目录结构

```bash
jarboot                             //当前工作流目录
├─logs                              //日志默认记录在用户目录下的jarboot文件夹中
│
├─jarboot-agent.jar                 //必要的文件不可少
│
├─jarboot-core.jar                  //注入目标服务的核心实现
│
├─jarboot-service.jar               //Web服务HTTP接口及WebSocket及主要业务实现
│
├─services                          //约定的管理其他jar文件的默认根目录(可配置)
│  ├─demo1-service                  //服务名为目录, 目录下存放启动的jar文件及其依赖
│  │   └─demo1-service.jar          //启动的jar文件, 若有多个则需要在[全局配置]界面配置启动的jar文件, 否则可能会随机选择一个
│  └─demo2-service                  //同上, 根目录下可放置很多个服务目录
│      └─demo2-service.jar
└─static                            //前端界面资源位置, jarboot-ui模块的编译打包的输出
   ├─index.html                     //打包后的html文件
   ├─umi.css                        //打包后的css文件
   └─umi.js                         //打包后的js文件
```
后端服务启动会指定一个管理其他启动jar文件的根路径（默认为当前路径下的services，可在【全局配置】界面配置），在此根目录下创建每个服务目录，创建的 ***目录名字为服务名*** ，在创建的目录下放入jar包文件，详细可见上面的目录结构约定。

3. 启动<code>jarboot-service.jar</code>主控服务
```
java -jar jarboot-service.jar
```

4. 浏览器访问<http://127.0.0.1:9899>

---
<span id="f1">1[](#a1)</span>: 淘宝的一个非常强大的Java调试工具。<br>
<span id="f2">2[](#a2)</span>: 可以配置优先级级别，从整数值1开始，越大约先启动，停止的顺序则相反，默认为1。<br>
<span id="f3">3[](#a3)</span>: 开发中可以由<code>gitlab runner</code>、<code>Jenkins</code>等工具自动构建后通过脚本拷贝到Jarboot指定的目录下，Jarboot监控到文件的更新会自动重启服务，目录监控实现了<code>防抖设计</code>（在一定时间内的多次更新只会触发一次重启）。
