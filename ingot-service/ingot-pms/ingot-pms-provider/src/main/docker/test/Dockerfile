# ====================================
# 测试环境 Dockerfile
# ====================================
# 特点：
# - 使用 jdk-slim 镜像
# - 中等内存配置
# - 启用基本的 JVM 调优
# - 使用 test profile
# - 包含健康检查
# ====================================

FROM amazoncorretto:17

# 数据卷
VOLUME ["/ingot-data"]

# 设置工作目录
WORKDIR /app

# 设置时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 创建数据目录和日志目录
RUN mkdir -p /ingot-data /app/logs

# 复制 jar 文件
COPY *.jar app.jar

# 暴露端口
EXPOSE 5200

# 环境变量
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xmx1g \
               -Xms512m \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/app/logs/heapdump.hprof \
               -Dspring.profiles.active=test \
               -Djava.security.egd=file:/dev/./urandom"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD curl -f http://localhost:5200/actuator/health || exit 1

# 启动命令
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
