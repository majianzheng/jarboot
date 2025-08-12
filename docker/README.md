## 使用`docker`

```shell
# 安装docker的部分就不做介绍了
# clone项目
# 从gitee clone
git clone https://gitee.com/majz0908/jarboot.git
# 从github clone
git clone https://github.com/majianzheng/jarboot.git

# 编译打包项目，需jdk17+、maven、nodejs16+
mvn clean install -P prod
sh docker/docker_image_build.sh

# 启动docker容器
sudo docker run -itd --name jarboot -p 9899:9899 jarboot
```

## 使用`docker compose`

```shell
# 安装docker的部分就不做介绍了
# clone项目
# 从gitee clone
git clone https://gitee.com/majz0908/jarboot.git
# 从github clone
git clone https://github.com/majianzheng/jarboot.git

cd jarboot

# 编译打包项目，需jdk17+、maven、nodejs16+
mvn clean install -P prod

cd docker

# 构建jarboot镜像
sudo bash docker_image_build.sh

#  初始化docker目录
sudo bash init_docker_dir.sh

# vi .env文件，可通过修改环境变量配置来修改默认的用户名和密码，默认用户名：jarboot 密码：jarboot

# 启动jarboot docker compose，单机版可指定使用【docker-compose-standalone.yml】文件
sudo docker compose up -d

```