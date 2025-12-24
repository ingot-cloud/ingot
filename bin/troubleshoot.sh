#!/bin/bash

# ====================================
# Java 服务故障诊断脚本
# ====================================
# 用途：快速诊断服务假死问题
# 使用：./troubleshoot.sh [容器名称]
# 示例：./troubleshoot.sh ingot-pms
# ====================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 默认容器名称
CONTAINER_NAME="${1:-ingot-pms}"
OUTPUT_DIR="./troubleshoot_$(date +%Y%m%d_%H%M%S)"

# 打印信息函数
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_section() {
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}$1${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
}

# 检查容器是否存在
check_container() {
    if ! docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        print_error "容器 ${CONTAINER_NAME} 不存在"
        exit 1
    fi
    
    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        print_warning "容器 ${CONTAINER_NAME} 未运行"
        return 1
    fi
    
    return 0
}

# 创建输出目录
create_output_dir() {
    mkdir -p "$OUTPUT_DIR"
    print_info "诊断结果将保存到: $OUTPUT_DIR"
}

# 1. 容器基本信息
collect_container_info() {
    print_section "1. 收集容器基本信息"
    
    print_info "容器状态..."
    docker ps -a --filter "name=${CONTAINER_NAME}" > "${OUTPUT_DIR}/container_status.txt"
    
    print_info "容器资源使用情况..."
    docker stats "${CONTAINER_NAME}" --no-stream > "${OUTPUT_DIR}/container_stats.txt"
    
    print_info "容器详细信息..."
    docker inspect "${CONTAINER_NAME}" > "${OUTPUT_DIR}/container_inspect.json"
    
    print_success "容器基本信息收集完成"
}

# 2. 容器日志
collect_logs() {
    print_section "2. 收集容器日志"
    
    print_info "最近 5000 行日志..."
    docker logs --tail 5000 "${CONTAINER_NAME}" > "${OUTPUT_DIR}/container_logs.txt" 2>&1 || true
    
    print_info "查找异常..."
    docker logs --tail 5000 "${CONTAINER_NAME}" 2>&1 | grep -i "exception\|error" > "${OUTPUT_DIR}/errors.txt" || true
    
    print_info "查找超时..."
    docker logs --tail 5000 "${CONTAINER_NAME}" 2>&1 | grep -i "timeout" > "${OUTPUT_DIR}/timeouts.txt" || true
    
    print_info "查找 OOM..."
    docker logs --tail 5000 "${CONTAINER_NAME}" 2>&1 | grep -i "OutOfMemoryError" > "${OUTPUT_DIR}/oom.txt" || true
    
    print_success "容器日志收集完成"
}

# 3. JVM 诊断
collect_jvm_info() {
    print_section "3. 收集 JVM 诊断信息"
    
    # 获取 Java 进程 PID
    PID=$(docker exec "${CONTAINER_NAME}" sh -c 'pgrep java' 2>/dev/null || echo "")
    
    if [ -z "$PID" ]; then
        print_warning "未找到 Java 进程，跳过 JVM 诊断"
        return
    fi
    
    print_info "Java 进程 PID: $PID"
    echo "PID: $PID" > "${OUTPUT_DIR}/jvm_pid.txt"
    
    # jstack - 线程堆栈
    print_info "导出线程堆栈（3次，间隔3秒）..."
    for i in 1 2 3; do
        print_info "  第 ${i} 次..."
        docker exec "${CONTAINER_NAME}" sh -c "jstack $PID" > "${OUTPUT_DIR}/jstack_${i}.txt" 2>&1 || true
        [ $i -lt 3 ] && sleep 3
    done
    
    # 统计线程状态
    print_info "统计线程状态..."
    grep "java.lang.Thread.State" "${OUTPUT_DIR}/jstack_1.txt" 2>/dev/null | sort | uniq -c > "${OUTPUT_DIR}/thread_states.txt" || true
    
    # 查找死锁
    print_info "检查死锁..."
    grep -A 20 "Found one Java-level deadlock" "${OUTPUT_DIR}/jstack_1.txt" > "${OUTPUT_DIR}/deadlocks.txt" 2>/dev/null || echo "未发现死锁" > "${OUTPUT_DIR}/deadlocks.txt"
    
    # jmap - 堆内存
    print_info "导出堆内存信息..."
    docker exec "${CONTAINER_NAME}" sh -c "jmap -heap $PID" > "${OUTPUT_DIR}/jmap_heap.txt" 2>&1 || true
    
    # jmap - 对象统计
    print_info "导出对象统计（前100）..."
    docker exec "${CONTAINER_NAME}" sh -c "jmap -histo:live $PID" 2>&1 | head -n 100 > "${OUTPUT_DIR}/jmap_histo.txt" || true
    
    # jstat - GC 统计
    print_info "导出 GC 统计（10次，间隔1秒）..."
    docker exec "${CONTAINER_NAME}" sh -c "jstat -gc $PID 1000 10" > "${OUTPUT_DIR}/jstat_gc.txt" 2>&1 || true
    docker exec "${CONTAINER_NAME}" sh -c "jstat -gcutil $PID 1000 10" > "${OUTPUT_DIR}/jstat_gcutil.txt" 2>&1 || true
    
    print_success "JVM 诊断信息收集完成"
}

# 4. 系统资源
collect_system_info() {
    print_section "4. 收集系统资源信息"
    
    print_info "CPU 使用情况..."
    docker exec "${CONTAINER_NAME}" sh -c "top -bn1" > "${OUTPUT_DIR}/top.txt" 2>&1 || true
    
    print_info "内存使用情况..."
    docker exec "${CONTAINER_NAME}" sh -c "free -h" > "${OUTPUT_DIR}/memory.txt" 2>&1 || true
    
    print_info "进程信息..."
    docker exec "${CONTAINER_NAME}" sh -c "ps aux" > "${OUTPUT_DIR}/processes.txt" 2>&1 || true
    
    print_success "系统资源信息收集完成"
}

# 5. 网络连接
collect_network_info() {
    print_section "5. 收集网络连接信息"
    
    print_info "当前连接数..."
    docker exec "${CONTAINER_NAME}" sh -c "netstat -antp 2>/dev/null || ss -antp" > "${OUTPUT_DIR}/connections.txt" 2>&1 || true
    
    print_info "连接统计..."
    docker exec "${CONTAINER_NAME}" sh -c "netstat -s 2>/dev/null || ss -s" > "${OUTPUT_DIR}/connection_stats.txt" 2>&1 || true
    
    print_info "TCP 连接状态分布..."
    docker exec "${CONTAINER_NAME}" sh -c "netstat -n 2>/dev/null | awk '/^tcp/ {++S[\$NF]} END {for(a in S) print a, S[a]}'" > "${OUTPUT_DIR}/tcp_states.txt" 2>&1 || true
    
    print_success "网络连接信息收集完成"
}

# 6. 应用日志文件
collect_app_logs() {
    print_section "6. 收集应用日志文件"
    
    print_info "检查日志目录..."
    docker exec "${CONTAINER_NAME}" sh -c "ls -lh /app/logs/" > "${OUTPUT_DIR}/log_files.txt" 2>&1 || print_warning "日志目录不存在或无法访问"
    
    print_info "复制应用日志..."
    docker cp "${CONTAINER_NAME}:/app/logs/" "${OUTPUT_DIR}/app_logs/" 2>&1 || print_warning "无法复制应用日志"
    
    print_success "应用日志文件收集完成"
}

# 7. 数据库连接检查（如果可访问）
check_database() {
    print_section "7. 数据库连接检查"
    
    print_info "提取数据库配置..."
    docker exec "${CONTAINER_NAME}" sh -c "env | grep -i mysql" > "${OUTPUT_DIR}/db_env.txt" 2>&1 || echo "未找到数据库环境变量" > "${OUTPUT_DIR}/db_env.txt"
    
    print_warning "请手动检查数据库连接数："
    echo "  1. 登录数据库：docker exec -it mysql mysql -uroot -p"
    echo "  2. 执行命令：SHOW PROCESSLIST;"
    echo "  3. 执行命令：SHOW STATUS LIKE 'Threads_connected';"
    
    print_success "数据库检查提示完成"
}

# 8. 生成分析报告
generate_report() {
    print_section "8. 生成分析报告"
    
    REPORT_FILE="${OUTPUT_DIR}/REPORT.md"
    
    cat > "$REPORT_FILE" << EOF
# 故障诊断报告

**容器名称**: ${CONTAINER_NAME}  
**诊断时间**: $(date '+%Y-%m-%d %H:%M:%S')  
**输出目录**: ${OUTPUT_DIR}

---

## 1. 快速检查清单

### 容器状态
\`\`\`
$(cat "${OUTPUT_DIR}/container_status.txt" 2>/dev/null || echo "无数据")
\`\`\`

### 资源使用
\`\`\`
$(cat "${OUTPUT_DIR}/container_stats.txt" 2>/dev/null || echo "无数据")
\`\`\`

---

## 2. 线程状态分布

\`\`\`
$(cat "${OUTPUT_DIR}/thread_states.txt" 2>/dev/null || echo "无数据")
\`\`\`

**关注点**：
- BLOCKED 线程数量：应该很少
- WAITING 线程数量：注意是否异常增多
- RUNNABLE 线程数量：正常工作的线程

---

## 3. 死锁检查

\`\`\`
$(cat "${OUTPUT_DIR}/deadlocks.txt" 2>/dev/null || echo "无数据")
\`\`\`

---

## 4. 堆内存使用

\`\`\`
$(cat "${OUTPUT_DIR}/jmap_heap.txt" 2>/dev/null | grep -A 20 "Heap Configuration\|Heap Usage" || echo "无数据")
\`\`\`

**关注点**：
- 老年代使用率：持续接近 100% 表示内存泄漏
- Eden 区使用率：频繁 Young GC
- Full GC 次数和时间：频繁 Full GC 是性能杀手

---

## 5. GC 统计

\`\`\`
$(cat "${OUTPUT_DIR}/jstat_gcutil.txt" 2>/dev/null || echo "无数据")
\`\`\`

**关注点**：
- FGC（Full GC 次数）：应该很少
- FGCT（Full GC 总时间）：占用总时间比例
- OU（老年代使用率）：持续接近 100% 需要关注

---

## 6. 错误日志摘要

### 异常
\`\`\`
$(cat "${OUTPUT_DIR}/errors.txt" 2>/dev/null | head -n 50 || echo "无数据")
\`\`\`

### 超时
\`\`\`
$(cat "${OUTPUT_DIR}/timeouts.txt" 2>/dev/null | head -n 20 || echo "无数据")
\`\`\`

### OOM
\`\`\`
$(cat "${OUTPUT_DIR}/oom.txt" 2>/dev/null || echo "无数据")
\`\`\`

---

## 7. TCP 连接状态

\`\`\`
$(cat "${OUTPUT_DIR}/tcp_states.txt" 2>/dev/null || echo "无数据")
\`\`\`

**关注点**：
- ESTABLISHED：活动连接数
- TIME_WAIT：短连接过多会导致端口耗尽
- CLOSE_WAIT：应用未正确关闭连接

---

## 8. 下一步排查建议

### 如果是内存问题
1. 分析堆转储：\`jmap -dump:live,format=b,file=heapdump.hprof <PID>\`
2. 使用 Eclipse MAT 或 VisualVM 分析堆转储
3. 查找内存泄漏点

### 如果是线程问题
1. 对比 3 次线程堆栈，找出一直在运行的线程
2. 分析死锁信息，调整锁顺序
3. 检查是否有线程池耗尽

### 如果是数据库问题
1. 检查数据库连接池使用率（Druid 监控页面）
2. 查找慢 SQL（应用日志或数据库慢查询日志）
3. 检查数据库连接数和锁等待

### 如果是 GC 问题
1. 分析 GC 日志：\`/app/logs/gc_*.log\`
2. 调整 JVM 参数（堆大小、GC 算法）
3. 优化代码，减少对象创建

---

## 9. 相关文件

- 容器状态：\`container_status.txt\`
- 容器日志：\`container_logs.txt\`
- 线程堆栈：\`jstack_1.txt\`, \`jstack_2.txt\`, \`jstack_3.txt\`
- 堆内存：\`jmap_heap.txt\`, \`jmap_histo.txt\`
- GC 统计：\`jstat_gc.txt\`, \`jstat_gcutil.txt\`
- 应用日志：\`app_logs/\`

---

**参考文档**: docs/TROUBLESHOOTING-SERVICE-HANG.md
EOF

    print_success "分析报告已生成: $REPORT_FILE"
}

# 主函数
main() {
    print_section "Java 服务故障诊断工具"
    
    print_info "目标容器: ${CONTAINER_NAME}"
    
    # 检查容器
    if ! check_container; then
        print_error "容器检查失败，退出"
        exit 1
    fi
    
    # 创建输出目录
    create_output_dir
    
    # 开始收集信息
    collect_container_info
    collect_logs
    collect_jvm_info
    collect_system_info
    collect_network_info
    collect_app_logs
    check_database
    generate_report
    
    # 完成
    print_section "诊断完成"
    print_success "所有诊断信息已保存到: ${OUTPUT_DIR}"
    print_info "查看分析报告: cat ${OUTPUT_DIR}/REPORT.md"
    print_info ""
    print_info "下一步："
    print_info "  1. 查看分析报告了解系统状态"
    print_info "  2. 根据建议进行进一步排查"
    print_info "  3. 参考文档：docs/TROUBLESHOOTING-SERVICE-HANG.md"
}

# 运行主函数
main

