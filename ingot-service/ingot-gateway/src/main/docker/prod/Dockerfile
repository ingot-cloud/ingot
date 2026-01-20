# ====================================
# 生产环境 Dockerfile
# ====================================

FROM docker-registry.ingotcloud.top/amazoncorretto:21

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
EXPOSE 7980

# 环境变量 - 生产环境完整的 JVM 调优
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-server \
               -Xms2g \
               -Xmx2g \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:InitiatingHeapOccupancyPercent=45 \
               -XX:MetaspaceSize=256m \
               -XX:MaxMetaspaceSize=256m \
               -XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75 \
               -XX:+AlwaysPreTouch \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/app/logs/heapdump.hprof \
               -XX:+ExitOnOutOfMemoryError \
               -Xlog:gc*,gc+heap=info,gc+age=trace:file=/app/logs/gc.log:time,level,tags:filecount=5,filesize=10M \
               -Dspring.profiles.active=prod \
               -Duser.timezone=Asia/Shanghai"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=90s \
    CMD curl -f http://localhost:8980/actuator/health || exit 1

# 启动命令
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
