# Jarboot service

### 主要功能
* Web服务器，实现<code>http</code>接口，及前端资源代理
* 作为WebSocket服务端与其管理的其他Java进程客户端交互
* 权限认证，部分命令和操作需要认证用户权限

### 架构
```
Modules
┏━━━━━━━━━━━━━━━━━┓
┃    Browser      ┃          ┏━━━━━━━━━━━━━━━━━┓
┗━━━━━━━━┯━━━━━━━━┛    ╭—————┨ jarboot-agent   ┠——————————————╮
     http│websocket    │     ┗━━━━━━━━━━━━━━━━━┛              │JarbootClassLoader
         │             │                                      │
         ↓             ↓                                      ↓
┏━━━━━━━━━━━━━━━━━┓ attach   ┏━━━━━━━━━━━━━━━━━┓      ┏━━━━━━━━━━━━━━━━━┓
┃ jarboot-service ┠—————————→┃ target process  ┠—————→┃ jarboot-core    ┃←————╮
┗━━━━━━━━━━━━━━━━━┛          ┗━━━━━━━━━━━━━━━━━┛      ┗━━━━━━━━┯━━━━━━━━┛     │
         ↑                                                     │              │
         ╰—————————————————————————————————————————————————————╯              │
              http and websocket connect to jarboot-service                   │
                                                      ┏━━━━━━━━━━━━━━━━━┓     │
                                                      ┃   jarboot-spy   ┠—————╯
                                                      ┗━━━━━━━━━━━━━━━━━┛ 


Command execute
                                                 websocket
┏━━━━━━━━━━━━━━━━━┓  websocket ┏━━━━━━━━━━━━━━━━━┓ send   ┏━━━━━━━━━━━━━━━━━━━━━━━┓
┃    Browser      ┃←——————————→┃ jarboot-service ┠———————→┃    target process     ┃
┗━━━━━━━━━━━━━━━━━┛ send/recv  ┗━━━━━━━━━━━━━━━━━┛        ┃   ╭——————————————╮    ┃
                                       ↑                  ┃   │  user code   │    ┃
                                       │                  ┃   ╰——————————————╯    ┃
                                  http │ websocket        ┃   ╭——————————————╮    ┃
                                       ╰——————————————————┃———┥ jarboot-core │    ┃
                                       result back        ┃   ╰——————————————╯    ┃
                                                          ┗━━━━━━━━━━━━━━━━━━━━━━━┛
```

### 编译，在项目根目录
```bash
$ mvn clean install
```
### 生产环境配置及使用
配置文件<code>jarboot.properties</code>在与jar包同级的目录下，启动前建议修改jarboot.token.secret.key
