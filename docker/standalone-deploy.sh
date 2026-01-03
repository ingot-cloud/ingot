#!/usr/bin/env bash

# ====================================
# Docker Compose 单机部署脚本
# ====================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示帮助信息
show_help() {
    cat << EOF
Docker Compose 单机部署脚本

用法：
    $0 [命令] [选项]

命令：
    deploy [compose-file]              部署所有服务
                                       (默认: docker-compose.standalone.yml)
    start [service] [compose-file]     启动服务（默认：所有服务）
    stop [service] [compose-file]      停止服务（默认：所有服务）
    restart [service] [compose-file]   重启服务（默认：所有服务）
    status [compose-file]              查看服务状态
    logs [service] [compose-file]      查看服务日志
    ps [compose-file]                  查看运行的容器
    down [compose-file]                停止并移除所有容器
    clean [compose-file]               清理服务（包括数据卷）
    network-create                     创建 ingot-net 网络
    network-remove                     删除 ingot-net 网络
    help                               显示此帮助信息

可用服务名称：
    ingot-auth      # Auth 服务
    ingot-gateway   # Gateway 服务
    ingot-member    # Member 服务
    ingot-pms       # PMS 服务

示例：
    # ========== 网络管理 ==========
    # 创建网络
    $0 network-create

    # ========== 服务部署 ==========
    # 部署所有服务（使用默认配置）
    $0 deploy

    # 部署服务（使用自定义配置文件）
    $0 deploy docker-compose.custom.yml

    # ========== 服务启动 ==========
    # 启动所有服务
    $0 start

    # 启动单个服务（Gateway）
    $0 start ingot-gateway

    # ========== 服务停止 ==========
    # 停止所有服务
    $0 stop

    # 停止单个服务（Auth）
    $0 stop ingot-auth

    # ========== 服务重启 ==========
    # 重启所有服务
    $0 restart

    # 重启单个服务（Member）
    $0 restart ingot-member

    # ========== 状态查看 ==========
    # 查看服务状态
    $0 status

    # 查看Gateway日志
    $0 logs ingot-gateway

    # ========== 清理 ==========
    # 清理所有服务和数据
    $0 clean

环境变量配置：
    1. 复制环境变量模板：
       cp env-templates/env.8c16g.template .env

    2. 编辑 .env 文件，修改配置参数

    3. 运行部署命令
EOF
}

# 检查环境变量文件
check_env_file() {
    if [ ! -f ".env" ]; then
        log_warning ".env 文件不存在"
        log_info "建议从模板复制：cp env-templates/env.8c16g.template .env"
        echo ""
        read -p "是否继续使用默认配置？(y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "已取消操作"
            exit 0
        fi
    else
        log_success "找到 .env 配置文件"
    fi
}

# 检查必要文件
check_required_files() {
    local compose_file=$1

    if [ ! -f "$compose_file" ]; then
        log_error "配置文件 $compose_file 不存在"
        exit 1
    fi

    if [ ! -d "services-env" ]; then
        log_error "services-env 目录不存在"
        exit 1
    fi

    log_success "配置文件检查通过"
}

# 创建网络
create_network() {
    log_info "检查 Docker 网络..."

    if docker network ls | grep -q "ingot-net"; then
        log_warning "网络 ingot-net 已存在"
    else
        log_info "创建 Docker 网络: ingot-net"
        docker network create --driver bridge ingot-net
        log_success "网络创建成功"
    fi
}

# 删除网络
remove_network() {
    log_info "删除 Docker 网络: ingot-net"

    if docker network ls | grep -q "ingot-net"; then
        docker network rm ingot-net 2>/dev/null || log_warning "网络可能正在使用中，请先停止相关服务"
        log_success "网络删除成功"
    else
        log_warning "网络 ingot-net 不存在"
    fi
}

# 部署服务
deploy_services() {
    local compose_file="${1:-docker-compose.standalone.yml}"

    log_info "开始部署 Ingot Cloud 服务..."
    log_info "使用配置文件: $compose_file"

    check_env_file
    check_required_files "$compose_file"
    create_network

    # 显示当前配置
    if [ -f ".env" ]; then
        source .env
        echo ""
        log_info "当前配置："
        echo "  - 镜像仓库: ${REGISTRY_URL:-docker-registry.ingotcloud.top}"
        echo "  - 版本: ${VERSION:-0.1.0}"
        echo "  - Gateway端口: ${GATEWAY_PORT:-7980}"
        echo "  - 数据卷: ${DOCKER_VOLUME:-/ingot-data}"
        echo ""
    fi

    log_info "拉取最新镜像..."
    docker compose -f "$compose_file" pull

    log_info "启动服务..."
    docker compose -f "$compose_file" up -d

    echo ""
    log_success "服务部署完成！"
    echo ""
    log_info "查看服务状态："
    echo "    $0 status"
    echo ""
    log_info "查看服务日志："
    echo "    $0 logs <service-name>"
}

# 启动服务
start_services() {
    local service=""
    local compose_file="docker-compose.standalone.yml"

    # 解析参数：第一个参数可能是服务名或配置文件
    if [ -n "$1" ]; then
        if [[ "$1" == *.yml ]] || [[ "$1" == *.yaml ]]; then
            # 第一个参数是配置文件
            compose_file="$1"
        else
            # 第一个参数是服务名
            service="$1"
            # 第二个参数可能是配置文件
            if [ -n "$2" ]; then
                compose_file="$2"
            fi
        fi
    fi

    check_required_files "$compose_file"

    if [ -n "$service" ]; then
        log_info "启动服务: $service"
        docker compose -f "$compose_file" start "$service"
        log_success "服务 $service 已启动"
    else
        log_info "启动所有服务..."
        docker compose -f "$compose_file" start
        log_success "所有服务已启动"
    fi
}

# 停止服务
stop_services() {
    local service=""
    local compose_file="docker-compose.standalone.yml"

    # 解析参数：第一个参数可能是服务名或配置文件
    if [ -n "$1" ]; then
        if [[ "$1" == *.yml ]] || [[ "$1" == *.yaml ]]; then
            # 第一个参数是配置文件
            compose_file="$1"
        else
            # 第一个参数是服务名
            service="$1"
            # 第二个参数可能是配置文件
            if [ -n "$2" ]; then
                compose_file="$2"
            fi
        fi
    fi

    check_required_files "$compose_file"

    if [ -n "$service" ]; then
        log_info "停止服务: $service"
        docker compose -f "$compose_file" stop "$service"
        log_success "服务 $service 已停止"
    else
        log_info "停止所有服务..."
        docker compose -f "$compose_file" stop
        log_success "所有服务已停止"
    fi
}

# 重启服务
restart_services() {
    local service=""
    local compose_file="docker-compose.standalone.yml"

    # 解析参数：第一个参数可能是服务名或配置文件
    if [ -n "$1" ]; then
        if [[ "$1" == *.yml ]] || [[ "$1" == *.yaml ]]; then
            # 第一个参数是配置文件
            compose_file="$1"
        else
            # 第一个参数是服务名
            service="$1"
            # 第二个参数可能是配置文件
            if [ -n "$2" ]; then
                compose_file="$2"
            fi
        fi
    fi

    check_required_files "$compose_file"

    if [ -n "$service" ]; then
        log_info "重启服务: $service"
        docker compose -f "$compose_file" restart "$service"
        log_success "服务 $service 已重启"
    else
        log_info "重启所有服务..."
        docker compose -f "$compose_file" restart
        log_success "所有服务已重启"
    fi
}

# 查看服务状态
show_status() {
    local compose_file="${1:-docker-compose.standalone.yml}"

    log_info "服务状态："
    echo ""

    if [ -f "$compose_file" ]; then
        docker compose -f "$compose_file" ps
    else
        docker ps --filter "name=ingot-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    fi
}

# 查看服务日志
show_logs() {
    local service=$1
    local compose_file="${2:-docker-compose.standalone.yml}"

    if [ -z "$service" ]; then
        log_error "请指定服务名称"
        echo "用法: $0 logs <service-name>"
        echo ""
        echo "可用服务："
        echo "  - ingot-auth"
        echo "  - ingot-gateway"
        echo "  - ingot-member"
        echo "  - ingot-pms"
        exit 1
    fi

    log_info "查看服务日志: $service"
    check_required_files "$compose_file"

    docker compose -f "$compose_file" logs -f "$service"
}

# 查看运行的容器
show_ps() {
    local compose_file="${1:-docker-compose.standalone.yml}"

    log_info "运行中的容器："
    echo ""
    check_required_files "$compose_file"

    docker compose -f "$compose_file" ps -a
}

# 停止并移除服务
down_services() {
    local compose_file="${1:-docker-compose.standalone.yml}"

    log_info "停止并移除服务..."
    check_required_files "$compose_file"

    docker compose -f "$compose_file" down
    log_success "服务已移除"
}

# 清理服务（包括数据卷）
clean_services() {
    local compose_file="${1:-docker-compose.standalone.yml}"

    log_warning "⚠️  此操作将删除所有服务和数据卷！"
    read -p "确认继续？(yes/no) " -r
    echo

    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        log_info "已取消操作"
        exit 0
    fi

    log_info "清理服务和数据..."
    check_required_files "$compose_file"

    docker compose -f "$compose_file" down -v
    log_success "服务和数据已清理"
}

# 主函数
main() {
    local command=${1:-help}

    case "$command" in
        deploy)
            deploy_services "$2"
            ;;
        start)
            start_services "$2" "$3"
            ;;
        stop)
            stop_services "$2" "$3"
            ;;
        restart)
            restart_services "$2" "$3"
            ;;
        status)
            show_status "$2"
            ;;
        logs)
            show_logs "$2" "$3"
            ;;
        ps)
            show_ps "$2"
            ;;
        down)
            down_services "$2"
            ;;
        clean)
            clean_services "$2"
            ;;
        network-create)
            create_network
            ;;
        network-remove)
            remove_network
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            log_error "未知命令: $command"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"

