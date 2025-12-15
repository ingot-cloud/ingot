# ====================================
# 生产环境 Dockerfile
# ====================================
# 特点：
# - 使用 alpine 镜像（更小更安全）
# - 较大的内存配置
# - 完整的 JVM 性能调优
# - 使用非 root 用户运行
# - 使用 prod profile
# - 完善的健康检查
# - 支持多架构（amd64/arm64）
# ====================================

FROM amazoncorretto:17-alpine

# 数据卷
VOLUME ["/ingot-data"]

# 设置工作目录
WORKDIR /app

# 安装必要工具
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 创建非 root 用户
RUN addgroup -S spring && adduser -S spring -G spring

# 复制 jar 文件
COPY *.jar app.jar

# 创建数据目录和日志目录并设置权限
RUN mkdir -p /ingot-data /app/logs && \
    chown -R spring:spring /app /ingot-data

# 切换到非 root 用户
USER spring:spring

# 暴露端口
EXPOSE 5100

# 环境变量 - 生产环境完整的 JVM 调优
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-server \
               -Xmx2g \
               -Xms1g \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:ParallelGCThreads=4 \
               -XX:ConcGCThreads=2 \
               -XX:InitiatingHeapOccupancyPercent=45 \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/app/logs/heapdump.hprof \
               -XX:+PrintGCDetails \
               -XX:+PrintGCDateStamps \
               -XX:+PrintGCTimeStamps \
               -Xloggc:/app/logs/gc.log \
               -XX:+UseGCLogFileRotation \
               -XX:NumberOfGCLogFiles=5 \
               -XX:GCLogFileSize=10M \
               -Dspring.profiles.active=prod \
               -Djava.security.egd=file:/dev/./urandom \
               -Duser.timezone=Asia/Shanghai"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=90s \
    CMD curl -f http://localhost:5100/actuator/health || exit 1

# 启动命令
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
