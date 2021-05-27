# Jarboot ❤️

![logo](https://gitee.com/majz0908/jarboot/raw/master/doc/jarboot.png)

<code>Jarboot</code> 是一个管理、监控及调试一系列Java进程的工具

在测试环境、每日构建的集成测试环境，可以把一系列编译输出等jar文件放入指定等目录，由<code>Jarboot</code>提供友好的浏览器ui界面和http接口，统一管理它的启动、停止及状态的监控。界面同时集成了<code>Arthas</code><sup id="a1">[[1]](#f1)</sup> 调试工具（开发中），支持远程通过<code>Jarboot</code>的界面对目标进程调试。

## 技术背景及目标
<code>Jarboot</code> 使用<code>Java Agent</code>和<code>ASM</code>技术往目标Java进程注入代码，无业务侵入性，注入的代码目前仅用于和<code>Jarboot</code> 的服务保持连接，以确定其状态，以及Java进程的优雅退出，后续会陆续加入与<code>Arthas</code>类似的功能，如监控线程状态、获取线程栈信息等。但它的功能定位与<code>Arthas</code>不同，虽然使用了同样的技术（agent和asm），<code>Jarboot</code> 更偏向于面向开发、测试、集成等，Arthas主要面向开发、运维。

- 🌈浏览器界面管理，一键启、停服务(已实现)
- 🔥支持启动、停止优先级配置<sup id="a2">[[2]](#f2)</sup>（已实现）
- ⭐️支持进程守护，开启后若服务异常退出则自动启动并通知（已实现）
- ☀️支持文件更新监控，开启后若jar文件更新则自动重启<sup id="a3">[[3]](#f3)</sup>（已实现）
- ❤️远程调试、查看进程信息（已实现通过Arthas调试，在全局配置里配置Arthas路径或设置ARTHAS_HOME环境变量）
- 调试命令执行（开发中）

采用<code>前后端分离</code>架构，前端界面采用<code>React</code>技术，脚手架使用<code>Umi</code>，组件库使用Umi内置等<code>antd</code>。后端服务主要由<code>SpringBoot</code>实现，提供http接口和静态资源代理。通过<code>WebSocket</code>向前端界面实时推送进程信息，同时与启动的Java进程维持一个长连接，以监控其状态。

模块|描述
:-|:-
jarboot-common|公共代理工具类实现
jarboot-agent|agent的jar启动或运行中注入目标进程
jarboot-core|在目标进程中执行的代码，使用<code>Netty</code>与主控服务交互
jarboot-service|主控服务：核心业务逻辑实现，提供http和WebSocket
jarboot-ui|前端ui界面，使用<code>React</code>实现

## 使用方法
1. 编译java和前端项目
```
//切换到代码根目录
mvn clean install

cd jarboot-ui
yarn build
```

2. 将编译的输出放入如下的目录结构

```bash
root path
${user.home}/jarboot
├─logs
│
├─jarboot-agent.jar
│
├─jarboot-core.jar
│
├─jarboot-service.jar
│
├─services
│  ├─demo1-service
│  │   └─demo1-service.jar
│  └─demo2-service
│      └─demo2-service.jar
└─static
   ├─index.html
   ├─umi.css
   └─umi.js
```
后端服务启动会指定一个工作目录（默认为${user.home}/jarboot/services，可配置），在此目录下创建目录，创建的 ***目录名字为服务名*** ，在创建的目录下放入jar包文件。

3. 启动<code>jarboot-service.jar</code>主控服务
```
java -jar jarboot-service.jar
```

4. 浏览器访问<http://127.0.0.1:9899>

---
<span id="f1">1[](#a1)</span>: 淘宝的一个非常强大的Java调试工具。<br>
<span id="f2">2[](#a2)</span>: 可以配置优先级级别，从整数值1开始，越大约先启动，停止的顺序则相反，默认为1。<br>
<span id="f3">3[](#a3)</span>: 开发中可以由<code>gitlab runner</code>、<code>Jenkins</code>等工具自动构建后通过脚步拷贝到Jarboot指定等目录下，Jarboot监控到更新会自动重启服务。
