# Jarboot ❤️

![logo](https://gitee.com/majz0908/jarboot/raw/develop/doc/jarboot.png)

[![Java CI with Maven](https://github.com/majianzheng/jarboot/actions/workflows/maven.yml/badge.svg)](https://github.com/majianzheng/jarboot/actions/workflows/maven.yml)
[![CodeQL](https://github.com/majianzheng/jarboot/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/majianzheng/jarboot/actions/workflows/codeql-analysis.yml)
![Maven Central](https://img.shields.io/maven-central/v/io.github.majianzheng/jarboot-all)
[![Build Status](https://travis-ci.com/majianzheng/jarboot.svg?branch=master)](https://travis-ci.com/majianzheng/jarboot)
[![codecov](https://codecov.io/gh/majianzheng/jarboot/branch/master/graph/badge.svg?token=FP7EPSFH4E)](https://codecov.io/gh/majianzheng/jarboot)
![GitHub](https://img.shields.io/github/license/majianzheng/jarboot)
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/majianzheng/jarboot.svg)](http://isitmaintained.com/project/majianzheng/jarboot "Average time to resolve an issue")
[![Percentage of issues still open](http://isitmaintained.com/badge/open/majianzheng/jarboot.svg)](http://isitmaintained.com/project/majianzheng/jarboot "Percentage of issues still open")
[![语雀](https://img.shields.io/badge/%E8%AF%AD%E9%9B%80-%E6%96%87%E6%A1%A3%E7%A4%BE%E5%8C%BA-brightgreen.svg)](https://www.yuque.com/jarboot/usage/tmpomo)

<code>Jarboot</code> is a Java process starter，which can manage, monitor and debug a series of Java instance.

In the test environment and daily built integrated environment, a series of jar files such as compilation output can be put into the agreed directory. <code>Jarboot</code> provides a friendly browser UI interface and HTTP interface to manage its start, stop and status monitoring, and execute commands to debug the target process.

[中文说明/Chinese Documentation](README_CN.md)

📚 Document: https://www.yuque.com/jarboot/usage/tmpomo

😊 Advanced application examples: <code>Jarboot</code> 🔥 with <code>Spring Cloud Alibaba</code> Examples ⤵️ 

🍏 The Example url: https://github.com/majianzheng/jarboot-with-spring-cloud-alibaba-example ⭐️ 

![overview](https://gitee.com/majz0908/jarboot/raw/develop/doc/overview.png)

## Background and objectives
<code>Jarboot</code> uses Java agent and <code>ASM</code> technology to inject code into the target java process, which is non-invasive. The injected code is only used for command interaction with jarboot's service. Some commands modify the bytecode of the class for class enhancement. A command system similar to <code>Arthas</code> is added, such as acquiring JVM information, monitoring thread status, acquiring thread stack information, etc.

- 🌈   Browser interface management, one click start, stop, do not have to manually execute one by one.
- 🔥   Support start and stop priority configuration<sup id="a2">[[1]](#f1)</sup>, and default parallel start.
- ⭐   Process daemon. If the service exits abnormally after opening, it will be automatically started and notified.
- ☀️   Support file update monitoring, and restart automatically if jar file is updated after opening.<sup id="a3">[[2]](#f2)</sup>
- 🚀   Debug command execution, remote debugging multiple Java processes at the same time, the interface is more friendly.

Front-end interface adopts <code>React</code> technology, scaffold uses <code>UmiJs</code>, component library uses 
<code>UmiJs</code> built-in <code>antd</code>. The back-end service is mainly implemented by <code>Springboot</code>, which provides HTTP interface and static resource broker. The process information is pushed through <code>websocket</code> to the front-end interface in real time, and a long connection is maintained with the started java process to monitor its status.

### Architecture brief introduction [view](jarboot-server/README.md).

## Install or build
1. Build ui and <code>Java</code> code, or download the zip package.

- <a href="https://github.com/majianzheng/jarboot/releases" target="_blank">Download from Github</a>
- <a href="https://repo1.maven.org/maven2/io/github/majianzheng/jarboot-packaging/" target="_blank">Download from maven center</a>

```bash
#At first build ui
$ cd jarboot-ui
#First time, execute yarn or npm install
$ yarn

#execute compile, yarn build or npm run build, execute yarn start or npm run start at development mode.
$ yarn build

#Switch to the code root directory and compile the Java code
$ cd ../
$ mvn clean install
```

2. Directory structures after installation.

```
jarboot                             #Current working directory
├─logs                              #logs
├─conf                              #jarboot配置文件
├─jarboot-spy.jar
├─jarboot-agent.jar                 
├─jarboot-core.jar                  
├─jarboot-server.jar                #Web service
└─services                          #Default root directory which managing other jar files (configurable)
   ├─demo1-service                  #The service name is directory, which stores the jar files and their dependencies.
   │   └─demo1-service.jar          #The jar file, If there are more than one, you need to config by service configuration interface, otherwise may randomly run one
   └─demo2-service                  
       └─demo2-service.jar
```
Back end service startup specifies a root path to manage other startup jar files (Default is services in current path, you can config it in [Setting])，Create each service directory under this root directory,created ***Directory name is the name of service*** .Put the jar package file in the created directory. See the directory structure convention above for details.

3. Start <code>jarboot-service.jar</code>
```bash
#Execute startup.sh to start, use startup.cmd when in windows OS.
$ sh startup.sh
```

4. Browser access <http://127.0.0.1:9899>
5. Enter the login page. Initial username: <code>jarboot</code>, default password: <code>jarboot</code>

![login](https://gitee.com/majz0908/jarboot/raw/develop/doc/login.png)

## Command list
### bytes
View the class bytes，Usage：

```bash
jarboot$ bytes com.mz.jarboot.demo.DemoServerApplication
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
Turn on or off real-time display of standard output stream (initially off), it will be displayed on the front-end UI of the web in real time.
The output stream includes <code>System.out.println</code>, <code>System.err.println</code> and log printing information such as <code>logger.info("hello")</code> in the code.

Note: when your program log output is too frequent, it will consume performance to turn on the display. It is recommended to turn it on only when necessary and turn it off after use.
```bash
#Turn on real time display of standard output stream
jarboot$ stdout on

#Turn off real time display of standard output stream
jarboot$ stdout off

#Get current status on or off
jarboot$ stdout
```

### dashboard
This is the real time statistics dashboard for the current system，click x cancel.

![dashboard](https://gitee.com/majz0908/jarboot/raw/develop/doc/dashboard.png)
  
### jad
Decompile the specified classes.

```bash
jarboot$ jad [-c] java.lang.String
````
![jad](https://gitee.com/majz0908/jarboot/raw/develop/doc/jad.png)

### jvm
Check the current JVM’s info

```bash
jarboot$ jvm
````

### sc
Search any loaded class with detailed information.

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
method calling path, and output the time cost for each node in the path.

```bash
jarboot$ trace com.mz.jarboot.demo.DemoServerApplication add 
Affect(class count: 2 , method count: 1) cost in 63 ms, listenerId: 2
`---ts=2021-06-15 23:34:20;thread_name=http-nio-9900-exec-3;id=13;is_daemon=true;priority=5;TCCL=org.springframework.boot.web.embedded.tomcat.TomcatEmbeddedWebappClassLoader@4690b489
    `---[0.053485ms] com.mz.jarboot.demo.DemoServerApplication:add()
```
  
### watch
methods in data aspect including return values, exceptions and parameters
    
Watch the first parameter and thrown exception of `com.mz.jarboot.demo.DemoServerApplicatio#add` only if it throws exception.

```bash
jarboot$ watch `com.mz.jarboot.demo.DemoServerApplicatio add {params[0], throwExp} -e
Press x to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 65 ms.
ts=2018-09-18 10:26:28;result=@ArrayList[
    @RequestFacade[org.apache.catalina.connector.RequestFacade@79f922b2],
    @NullPointerException[java.lang.NullPointerException],
]
```
  
### thread
Check the basic info and stack trace of the target thread.

```bash
jarboot$ thread -n 3
"nioEventLoopGroup-2-1" Id=31 cpuUsage=0.37% deltaTime=0ms time=880ms RUNNABLE
    at sun.management.ThreadImpl.dumpThreads0(Native Method)
    at sun.management.ThreadImpl.getThreadInfo(ThreadImpl.java:448)
    at com.mz.jarboot.core.cmd.impl.ThreadCommand.processTopBusyThreads(ThreadCommand.java:209)
    at com.mz.jarboot.core.cmd.impl.ThreadCommand.run(ThreadCommand.java:120)
    at com.mz.jarboot.core.basic.EnvironmentContext.runCommand(EnvironmentContext.java:162)
    at com.mz.jarboot.core.cmd.CommandDispatcher.execute(CommandDispatcher.java:35)
    at com.mz.jarboot.core.server.JarbootBootstrap$1.onText(JarbootBootstrap.java:94)
    at com.mz.jarboot.core.ws.WebSocketClientHandler.channelRead0(WebSocketClientHandler.java:83)
    at io.netty.channel.SimpleChannelInboundHandler.channelRead(SimpleChannelInboundHandler.java:99)

"C2 CompilerThread1" [Internal] cpuUsage=3.14% deltaTime=6ms time=4599ms


"C2 CompilerThread0" [Internal] cpuUsage=2.28% deltaTime=4ms time=4692ms
```

#### Classloader

了解当前系统中有多少类加载器，以及每个加载器加载的类数量，帮助您判断是否有类加载器泄露。

```bash
jarboot$ classloader
name	                                                numberOfInstances	loadedCountTotal
org.springframework.boot.loader.LaunchedURLClassLoader	1	                3929
BootstrapClassLoader	                                1                	2623
com.mz.jarboot.agent.JarbootClassLoader             	1               	1780
sun.misc.Launcher$AppClassLoader                    	1               	59
sun.reflect.DelegatingClassLoader                 	58                	58
sun.misc.Launcher$ExtClassLoader                     	1	                18
Affect(row-cnt:6) cost in 35 ms.
```

### heapdump
dump java heap in hprof binary format, like jmap.

```bash
jarboot$ heapdump
````
![heap dump](https://gitee.com/majz0908/jarboot/raw/develop/doc/heapdump.png)

### sysprop
Examine the system properties from the target JVM

```bash
#Get all.
jarboot$ sysprop
#Get one property.
jarboot$ sysprop user.home
```


---
## Credit
### Projects

* [bytekit](https://github.com/alibaba/bytekit) Java Bytecode Kit.
* [Arthas](https://github.com/alibaba/arthas) Some command is developed on the source of <code>Arthas</code>.

## 仓库镜像

* [码云Jarboot](https://gitee.com/majz0908/jarboot)

---
<span id="f1">1[](#a1)</span>: You can configure the priority level, starting from the integer value of 1. The more you start first, the reverse is the order of stop. The default value is 1。<br>
<span id="f2">2[](#a2)</span>: In development, it can be built automatically by tools such as gitlab runner, Jenkins, etc. and copied to the directory specified by Jarboot through script. Updates monitored by Jarboot will restart the service automatically. Directory monitoring implements anti-shake design (multiple updates within a certain period of time will trigger only one restart)。
