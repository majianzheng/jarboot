# jarboot前端ui

---
## 使用umi脚手架编译打包
关于<a href="https://umijs.org/zh-CN/docs" target="_blank">umi</a>详见官方网站

安装依赖,

```bash
$ yarn
```

启动调试服务器，代理默认配置了9899端口,

```bash
$ yarn start
```
构建实际生产环境,

```bash
$ yarn build
```

构建后生成的的html、css和js文件会通过<code>postbuild</code>执行<code>develop.js</code>脚本自动拷贝到jarboot-service模块下的<code>static</code>目录中
