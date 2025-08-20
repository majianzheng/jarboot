# Jarboot ❤️

![logo](https://gitee.com/majz0908/jarboot/raw/develop/doc/jarboot.png)

[![CodeQL](https://github.com/majianzheng/jarboot/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/majianzheng/jarboot/actions/workflows/codeql-analysis.yml)
![Maven Central](https://img.shields.io/maven-central/v/io.github.majianzheng/jarboot-all)
[![Build Status](https://travis-ci.com/majianzheng/jarboot.svg?branch=master)](https://travis-ci.com/majianzheng/jarboot)
[![codecov](https://codecov.io/gh/majianzheng/jarboot/branch/master/graph/badge.svg?token=FP7EPSFH4E)](https://codecov.io/gh/majianzheng/jarboot)
![GitHub](https://img.shields.io/github/license/majianzheng/jarboot)
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/majianzheng/jarboot.svg)](http://isitmaintained.com/project/majianzheng/jarboot "Average time to resolve an issue")
[![Percentage of issues still open](http://isitmaintained.com/badge/open/majianzheng/jarboot.svg)](http://isitmaintained.com/project/majianzheng/jarboot "Percentage of issues still open")
[![语雀](https://img.shields.io/badge/%E8%AF%AD%E9%9B%80-%E6%96%87%E6%A1%A3%E7%A4%BE%E5%8C%BA-brightgreen.svg)](https://www.yuque.com/jarboot/usage/quick-start)
![Docker Pulls](https://img.shields.io/docker/pulls/mazheng0908/jarboot)

<code>Jarboot</code> 是一个Java进程启停、管理、诊断的平台，可以管理、守护、监控及诊断本地和远程的Java进程。

在测试环境、每日构建的集成环境，可以把一系列编译输出等jar文件放入约定的目录，由<code>Jarboot</code>提供友好的浏览器ui界面和<code>http</code>接口，统一管理它的启动、停止及状态的监控，以及执行命令对目标进程进行调试。

English version goes [here](README.md).

📚 文档：https://www.yuque.com/jarboot

🍏 最佳实践 🔥 : [Jarboot with Spring Cloud Alibaba Example](https://github.com/majianzheng/jarboot-with-spring-cloud-alibaba-example) ⭐️

🐳 可扩展: 同时支持<code>JDK SPI</code>和<code>Spring SPI</code>，支持插件式开发。

📦 安装包下载: [https://gitee.com/majz0908/jarboot/releases](https://gitee.com/majz0908/jarboot/releases)

📺 视频演示： [哔哩哔哩视频](https://www.bilibili.com/video/BV1KG411e7ip/?share_source=copy_web&vd_source=b901b6d8d17d4922a1229758fa74e46c)

![overview](https://gitee.com/majz0908/jarboot/raw/develop/doc/overview.png)

## 技术背景及目标
<code>Jarboot</code> 使用<code>Java Agent</code>和<code>ASM</code>技术往目标Java进程注入代码，无业务侵入性，注入的代码仅用于和
<code>Jarboot</code> 的服务实现命令交互，部分命令会修改类的字节码用于类增强，加入了与<code>Arthas</code>类似的命令系统，如获取JVM信息、
监控线程状态、获取线程栈信息等。

- 🌈   浏览器界面管理，一键启、停服务进程，不必挨个手动执行
- 🔥   支持启动、停止优先级配置<sup id="a2">[[1]](#f1)</sup>，默认并行启动
- ⭐️   支持进程守护，开启后若服务异常退出则自动启动并通知
- ☀️   支持文件更新监控，开启后若jar文件更新则自动重启<sup id="a3">[[2]](#f2)</sup>
- 🚀   调试命令执行，同时远程调试多个Java进程，界面更友好
- 💎   支持通过<code>SPI</code>自定义调试命令实现，支持开发插件

![online diagnose](https://gitee.com/majz0908/jarboot/raw/develop/doc/online-diagnose.png)

### 架构简介 
详细架构设计[查看](jarboot-server/README.md)

前端界面采用<code>Vue3</code>技术。 后端服务主要由<code>SpringBoot</code>实现，提供http接口和静态资源代理。通过<code>WebSocket</code>向前端界面实时推送进程信息，同时与启动的Java进程维持一个长连接，以监控其状态。

Chrome >=87
Firefox >=78
Safari >=14
Edge >=88

## 安装或编译构建
### 下载压缩包文件的方式安装，或者使用<code>Docker</code>
#### 1. 安装包下载：<a href="https://github.com/majianzheng/jarboot/releases" target="_blank">从Github下载</a>

#### 2. 使用<code>Docker</code>
```bash
# Docker镜像构建，jdk17+，maven，nodejs16+
mvn clean install -P prod
# 构建docker镜像
sh docker/docker_image_build.sh

# 启动容器
sudo docker run -itd --name jarboot -p 9899:9899 mazheng0908/jarboot
```

#### 3. 使用`docker compose`配置：
```shell
# 构建jarboot包，jdk17+，maven，nodejs16+
mvn clean install -P prod
# 构建docker镜像
cd docker
# 若在运行中，先关闭
docker compose down

sh docker_image_build.sh

# 启动 docker compose
bash init_docker_dir.sh
docker compose -f docker-compose.yml up -d
```
- 集群模式：[docker-compose.yml](docker/docker-compose.yml)
- 单机模式：[docker-compose-standalone.yml](docker/docker-compose-standalone.yml)


### 编译源码的步骤
使用压缩包安装或者<code>Docker</code>的时候忽略此步骤

编译Jarboot源代码
```bash
#首先需要准备JDK17+和nodeJS16+的开发环境，然后执行：
$ mvn clean install -P prod
```
### 启动<code>Jarboot</code>服务
如果是使用的<code>Docker</code>忽略此步骤。
```bash
#执行 startup.sh 启动, 在Windows系统上使用startup.cmd。
$ sh startup.sh
```

### 浏览器访问<http://127.0.0.1:9899>
进入登录界面，初始的用户名：<code>jarboot</code>，默认密码：<code>jarboot</code>

## SPI扩展，支持JDK和Spring的SPI
使用扩展可以自己实现命令，自己定义一个命令如何执行。并且，可以时应用启动完成快速的通知Jarboot服务，不需要等待没有控制台输出的时间。
### SpringBoot应用
1. 引入<code>spring-boot-starter-jarboot</code>依赖
```xml
<dependency>
    <groupId>io.github.majianzheng</groupId>
    <artifactId>spring-boot-starter-jarboot</artifactId>
    <version>${jarboot.version}</version>
</dependency>
```
2. 实现<code>CommandProcessor</code>SPI接口

同样的, 你也可以在方法上使用 <code>@Bean</code> 注解来定义命令处理器。<br>
如果没有使用<code>@Name</code>注解的话，将会默认使用Bean的名称作为命令的名称。
```java
@Name("spring.command.name")
@Summary("The command summary")
@Description("The command usage detail")
@Component
public class DemoServiceImpl implements DemoService, CommandProcessor {
  @Override
  public String process(CommandSession session, String[] args) {
    return "Spring boot Demo user-defined command using Spring SPI";
  }
  //implement other method...
}
```
当引入了<code>spring-boot-starter-jarboot</code>依赖后，将会增加2个Spring调试命令，<code>spring.bean</code>和<code>spring.env</code>
```shell
#spring.bean 用法：
$ spring.bean [-b <name>] [-d]
#示例：
# 获取所有的bean name
$ spring.bean
# 获取bean的信息
$ spring.bean -b beanName
# 获取bean的详细信息
$ spring.bean -b beanName -d

#sping.env 用法：
$ spring.env <name>
#示例：
$ spring.env spring.application.name
```

### 非SpringBoot应用
演示普通的非SpringBoot的应用如何使用。
#### 如何创建一个用户自定义的命令
1. 引入jarboot api的依赖
```xml
<dependency>
    <groupId>io.github.majianzheng</groupId>
    <artifactId>jarboot-api</artifactId>
    <scope>provided</scope>
    <version>${jarboot.version}</version>
</dependency>
```
2. 实现spi接口
```java
/**
 * 使用Name注解来定义一个命令的名字
 */
@Name("demo")
@Summary("The command summary")
@Description("The command usage detail")
public class DemoCommandProcessor implements CommandProcessor {
    @Override
    public String process(CommandSession session, String[] args) {
        return "demo SPI command result.";
    }
}
```
3. 创建JDK的spi定义文件

在目录<code>resources</code>/<code>META-INF</code>/<code>services</code>中创建名为
  <code>spi.cmd.io.github.majianzheng.jarboot.api.CommandProcessor</code>的文件，内容为类的全名。

#### 启动成功主动通知Jarboot服务
```java
public class DemoApplication {
    public static void main(String[] args) {
        // do something
        try {
            //Notify completion
            JarbootFactory.createAgentService().setStarted();
        } catch (Exception e) {
            log(e.getMessage());
        }
    }
}
```

## 工具
### 文件浏览器
![file_browse](https://gitee.com/majz0908/jarboot/raw/develop/doc/file-browse.png)

### 终端
![terminal](https://gitee.com/majz0908/jarboot/raw/develop/doc/terminal.png)

## 命令列表
### bytes
查看类的字节码，用法：
```bash
jarboot$ bytes io.github.majianzheng.jarboot.demo.DemoServerApplication
ClassLoader: org.springframework.boot.loader.LaunchedURLClassLoader@31221be2
------
getUser
L0
LINENUMBER 27 L0

...

ILOAD 1
ILOAD 2
IADD
IRETURN
L8
```

### stdout
开启或关闭标准输出流的实时显示（默认开启），将会在Web的前端ui界面上实时显示，输出流包括代码中的<code>System.out.println</code>、<code>System.err.println</code>
以及日志打印信息如<code>logger.info("hello")</code>。

注：该功能的实现机制经过精心设计，建议一直开启，对性能没有影响还可加速启动。
```bash
#开启标准输出流实时显示
jarboot$ stdout on

#关闭标准输出流实时显示
jarboot$ stdout off

#获取当前的状态，启动或关闭
jarboot$ stdout
```
  
### dashboard
当前系统的实时数据面板，点击按钮取消

![dashboard](https://gitee.com/majz0908/jarboot/raw/develop/doc/dashboard.png)
  
### jad 
反编译

```bash
jarboot$ jad [-c] java.lang.String
````
![jad](https://gitee.com/majz0908/jarboot/raw/develop/doc/jad.png)

### jvm
查看进程JVM属性信息

```bash
jarboot$ jvm
````

### sc
查找JVM中已经加载的类

```bash
$ sc -d org.springframework.web.context.support.XmlWebApplicationContext
 class-info        org.springframework.web.context.support.XmlWebApplicationContext
 code-source       /Users/xxx/work/test/WEB-INF/lib/spring-web-3.2.11.RELEASE.jar
 name              org.springframework.web.context.support.XmlWebApplicationContext
 isInterface       false
 isAnnotation      false
 isEnum            false
 isAnonymousClass  false
 isArray           false
 isLocalClass      false
 isMemberClass     false
 isPrimitive       false
 isSynthetic       false
 simple-name       XmlWebApplicationContext
 modifier          public
 annotation
 interfaces
 super-class       +-org.springframework.web.context.support.AbstractRefreshableWebApplicationContext
                     +-org.springframework.context.support.AbstractRefreshableConfigApplicationContext
                       +-org.springframework.context.support.AbstractRefreshableApplicationContext
                         +-org.springframework.context.support.AbstractApplicationContext
                           +-org.springframework.core.io.DefaultResourceLoader
                             +-java.lang.Object
 class-loader      +-org.apache.catalina.loader.ParallelWebappClassLoader
                     +-java.net.URLClassLoader@6108b2d7
                       +-sun.misc.Launcher$AppClassLoader@18b4aac2
                         +-sun.misc.Launcher$ExtClassLoader@1ddf84b8
 classLoaderHash   25131501

````
  
### trace
方法执行监控 
```bash
jarboot$ trace io.github.majianzheng.jarboot.demo.DemoServerApplication add 
Affect(class count: 2 , method count: 1) cost in 63 ms, listenerId: 2
`---ts=2021-06-15 23:34:20;thread_name=http-nio-9900-exec-3;id=13;is_daemon=true;priority=5;TCCL=org.springframework.boot.web.embedded.tomcat.TomcatEmbeddedWebappClassLoader@4690b489
    `---[0.053485ms] io.github.majianzheng.jarboot.demo.DemoServerApplication:add()
```
  
### watch
方法执行数据监测
    
观察方法 `io.github.majianzheng.jarboot.demo.DemoServerApplicatio#add` 执行的入参，仅当方法抛出异常时才输出。

```bash
jarboot$ watch io.github.majianzheng.jarboot.demo.DemoServerApplicatio add {params[0], throwExp} -e
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 65 ms.
ts=2018-09-18 10:26:28;result=@ArrayList[
    @RequestFacade[org.apache.catalina.connector.RequestFacade@79f922b2],
    @NullPointerException[java.lang.NullPointerException],
]
```
  
### thread
查看当前线程信息，查看线程的堆栈

```bash
jarboot$ thread -n 3
"nioEventLoopGroup-2-1" Id=31 cpuUsage=0.37% deltaTime=0ms time=880ms RUNNABLE
    at sun.management.ThreadImpl.dumpThreads0(Native Method)
    at sun.management.ThreadImpl.getThreadInfo(ThreadImpl.java:448)
    at impl.cmd.io.github.majianzheng.jarboot.core.ThreadCommand.processTopBusyThreads(ThreadCommand.java:209)
    at impl.cmd.io.github.majianzheng.jarboot.core.ThreadCommand.run(ThreadCommand.java:120)
    at basic.io.github.majianzheng.jarboot.core.EnvironmentContext.runCommand(EnvironmentContext.java:162)
    at cmd.io.github.majianzheng.jarboot.core.CommandRequestSubscriber.execute(CommandDispatcher.java:35)
    at server.io.github.majianzheng.jarboot.core.JarbootBootstrap$1.onText(JarbootBootstrap.java:94)
    at io.github.majianzheng.jarboot.core.ws.WebSocketClientHandler.channelRead0(WebSocketClientHandler.java:83)
    at io.netty.channel.SimpleChannelInboundHandler.channelRead(SimpleChannelInboundHandler.java:99)

"C2 CompilerThread1" [Internal] cpuUsage=3.14% deltaTime=6ms time=4599ms


"C2 CompilerThread0" [Internal] cpuUsage=2.28% deltaTime=4ms time=4692ms
```

#### Classloader

查看classloader的继承树，urls，类加载信息

```bash
jarboot$ classloader
name	                                                numberOfInstances	loadedCountTotal
org.springframework.boot.loader.LaunchedURLClassLoader	1	                3929
BootstrapClassLoader	                                1                	2623
io.github.majianzheng.jarboot.agent.JarbootClassLoader             	1               	1780
sun.misc.Launcher$AppClassLoader                    	1               	59
sun.reflect.DelegatingClassLoader                 	58                	58
sun.misc.Launcher$ExtClassLoader                     	1	                18
Affect(row-cnt:6) cost in 35 ms.
```

### heapdump
dump java heap, 类似jmap命令的heap dump功能。

```bash
jarboot$ heapdump
````
![heap dump](https://gitee.com/majz0908/jarboot/raw/develop/doc/heapdump.png)

### sysprop
查看进程系统属性信息

```bash
#获取全部
jarboot$ sysprop
#获取指定的属性
jarboot$ sysprop user.home
```

---
## Credit
### Projects

* [bytekit](https://github.com/alibaba/bytekit) Java Bytecode Kit.
* [Arthas](https://github.com/alibaba/arthas) 部分命令在<code>Arthas</code>源码的基础上二次开发。

## 联系
- 邮箱: 282295811@qq.com
- QQ群: 663881845
- QQ群已满，微信群二维码会过期，大家关注下抖音吧，关注后加入抖音的粉丝群
- 抖音号：1077242754

![抖音](https://gitee.com/majz0908/jarboot/raw/develop/doc/douyin.png)
![投喂](https://gitee.com/majz0908/jarboot/raw/develop/doc/touwei.png)

## 仓库镜像

* [码云Jarboot](https://gitee.com/majz0908/jarboot)

---
<span id="f1">1[](#a1)</span>: 可以配置优先级级别，从整数值1开始，越大约先启动，停止的顺序则相反。<br>
<span id="f2">2[](#a2)</span>: 开发中可以由<code>gitlab runner</code>、<code>Jenkins</code>等工具自动构建后通过脚本拷贝到Jarboot指定的目录下，Jarboot监控到文件的更新会自动重启服务，目录监控实现了<code>防抖设计</code>（在一定时间内的多次更新只会触发一次重启）。
