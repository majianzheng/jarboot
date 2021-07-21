# Jarboot server

### 主要功能（Major function）
* Web服务器，实现<code>http</code>接口，及前端资源代理
* 作为WebSocket服务端与其管理的其他Java进程客户端交互
* 权限认证，部分命令和操作需要认证用户权限

### 架构（Architecture）
#### 模块关系（Modules）
```
Modules 模块关系
┏━━━━━━━━━━━━━━━━━┓
┃    Browser      ┃          ┏━━━━━━━━━━━━━━━━━┓
┗━━━━━━━━┯━━━━━━━━┛    ╭─────┨ jarboot-agent   ┠──────────────╮
     http│websocket    │     ┗━━━━━━━━━━━━━━━━━┛              │JarbootClassLoader
         │             │                                      │
         ▼             ▼                                      ▼ core作为客户端反向连接jarboot-server
┏━━━━━━━━━━━━━━━━━┓ attach   ┏━━━━━━━━━━━━━━━━━┓      ┏━━━━━━━━━━━━━━━━━┓
┃ jarboot-server  ┠─────────>┃ target process  ┠─────>┃ jarboot-core    ┃<────╮
┗━━━━━━━━━━━━━━━━━┛          ┗━━━━━━━━━━━━━━━━━┛      ┗━━━━━━━━┯━━━━━━━━┛     │
         ▲                                                     │              │
         ╰─────────────────────────────────────────────────────╯              │ class loaded
              http and websocket connect to jarboot-server                    │
                                                      ┏━━━━━━━━━━━━━━━━━┓     │
                                                      ┃   jarboot-spy   ┠─────╯
                                                      ┗━━━━━━━━━━━━━━━━━┛ 


Command execute 命令执行
                                                 websocket
┏━━━━━━━━━━━━━━━━━┓  websocket ┏━━━━━━━━━━━━━━━━━┓ send   ┏━━━━━━━━━━━━━━━━━━━━━━━┓
┃    Browser      ┃<──────────>┃ jarboot-server  ┠───────>┃    target process     ┃
┗━━━━━━━━━━━━━━━━━┛ send/recv  ┗━━━━━━━━━━━━━━━━━┛        ┃   ╭──────────────╮    ┃
                                       ▲                  ┃   │  user code   │    ┃
                                       │                  ┃   ╰──────────────╯    ┃
                                  http │ websocket        ┃   ╭──────────────╮    ┃
                                       ╰──────────────────╂───┤ jarboot-core │    ┃
                                       result back        ┃   ╰──────────────╯    ┃
                                                          ┗━━━━━━━━━━━━━━━━━━━━━━━┛
```
#### 序列图（Sequence）
```
Execute command Sequence:
╭──────────────╮       ╭────────────────╮      ╭───────────────╮
│    Browser   │       │ jarboot-server │      │ target server │
╰──────┬───────╯       ╰───────┬────────╯      ╰──────┬────────╯
       │     send command      │                      │
       ├─────────────────────>╭┴╮   send command      │
       │                      │ ├───────────────────>╭┴╮
       │                      ╰┬╯                    │ ├─╮ Execute command and
       │                       │    Large data http  │ │ │ Render result
       │                      ╭┴╮<───────────────────┤ │<╯
       │                      │ │                    │ │
       │                      │ │     WebSocket      │ │
       │        push          │ │<───────────────────┤ │
      ╭┴╮<────────────────────┤ │                    ╰┬╯ 
      │ ├─╮                   ╰┬╯                     │
      │ │ │Render UI           │                      │          
      │ │ │                    │                      │
      │ │<╯                    │                      │ 
      ╰┬╯                      │                      │
       │                       │                      │
       
       
```
### 编译，在项目根目录（Compile，in project root path）
```bash
$ mvn clean install
```
### 生产环境配置及使用（Configuration and use of production environment）
配置文件<code>jarboot.properties</code>在与jar包同级的目录下的<code>conf</code>目录，建议启动前修改jarboot.token.secret.key，启动后
修改默认的密码。
