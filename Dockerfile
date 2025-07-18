# 使用官方 Eclipse Temurin JDK 8 镜像作为基础镜像
FROM eclipse-temurin:8-jdk-jammy
# author
MAINTAINER wanghao
# 设定环境变量
ENV TZ=Asia/Shanghai
# 指定时区 #验证码字体包
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
# 设置工作目录
WORKDIR /app
# 将本地构建好的 jar 文件复制到容器中
# 请确保 jar 文件名与实际构建输出一致，例如：compilerJava.jar
COPY ./target/*.jar app.jar
# 暴露应用运行的端口（默认 Spring Boot 端口）
EXPOSE 80
# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
