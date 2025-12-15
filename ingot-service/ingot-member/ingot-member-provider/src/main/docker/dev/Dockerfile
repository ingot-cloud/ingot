# ====================================
# 开发环境 Dockerfile
# ====================================
# 特点：
# - 使用 jdk-slim 镜像（包含完整调试工具）
# - 较小的内存配置
# - 启用远程调试
# - 使用 dev profile
# ====================================

FROM amazoncorretto:17

# 数据卷
VOLUME ["/ingot-data"]

# 设置工作目录
WORKDIR /app

# 安装调试工具（开发环境需要）并设置时区
RUN yum install -y curl nc tzdata && \
    yum clean all && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 创建数据目录
RUN mkdir -p /ingot-data

# 复制 jar 文件
COPY *.jar app.jar

# 暴露端口
EXPOSE 5300

# 环境变量
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xmx512m \
               -Xms256m \
               -Xdebug \
               -Dspring.profiles.active=dev \
               -Djava.security.egd=file:/dev/./urandom"

# 启动命令
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
