# 使用官方 Eclipse Temurin JDK 8 镜像作为基础镜像
FROM eclipse-temurin:8-jdk-jammy

# 作者信息
MAINTAINER wanghao

# 设置环境变量
ENV TZ=Asia/Shanghai \
    TERM=xterm \
    DEBIAN_FRONTEND=noninteractive

# 设置时区 + 更换源 + 安装调试工具（合并为一个 RUN）
RUN set -eux; \
    ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone; \
    curl -fsSL https://chsrc.run/posix | bash; \
    chsrc set ubuntu; \
    apt update; \
    apt install -y iputils-ping curl net-tools telnet dnsutils ncurses-bin

# 设置工作目录
WORKDIR /app

# 复制 jar 包
COPY ./target/*.jar app.jar

# 暴露端口
EXPOSE 80

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]