# ====================================
# 开发环境 Dockerfile
# ====================================

FROM docker-registry.ingotcloud.top/amazoncorretto:17

# 数据卷
VOLUME ["/ingot-data"]

# 设置工作目录
WORKDIR /app

# 设置时区（如需网络调试工具，可安装 nc：yum install -y nc）
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 创建数据目录
RUN mkdir -p /ingot-data

# 复制 jar 文件
COPY *.jar app.jar

# 暴露端口
EXPOSE 5100

# 环境变量
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xmx512m \
               -Xms256m \
               -Xdebug \
               -Dspring.profiles.active=dev \
               -Djava.security.egd=file:/dev/./urandom"

# 启动命令
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
