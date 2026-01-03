#!/usr/bin/env bash

# ====================================
# Docker Swarm 一键部署脚本
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

# 加载 .env 文件并导出环境变量
load_env_file() {
    if [ -f ".env" ]; then
        log_info "加载 .env 配置文件..."
        # 读取 .env 文件，过滤注释和空行，导出为环境变量
        set -a  # 自动导出所有变量
        source <(grep -v '^#' .env | grep -v '^$' | sed 's/\r$//')
        set +a  # 关闭自动导出
        log_success ".env 文件加载成功"
    else
        log_warning ".env 文件不存在，将使用默认配置"
        log_info "请从模板复制：cp env-templates/env.8c16g.template .env"
    fi
}

# 显示帮助信息
show_help() {
    cat << EOF
Docker Swarm 一键部署脚本

用法：
    $0 [命令] [选项]

命令：
    init-manager [advertise-addr]    初始化Swarm Manager节点
    init-worker  <join-token>        将当前节点加入Swarm作为Worker
    deploy [compose-file]            部署Ingot Cloud服务栈
                                     (默认: docker-compose.yml)
    update                           更新服务（滚动更新）
    scale <service> <replicas>       扩缩容指定服务
    status                           查看服务状态
    logs <service>                   查看服务日志
    stop                             停止并移除服务栈
    leave                            离开Swarm集群
    help                             显示此帮助信息

示例：
    # 在Manager节点初始化Swarm
    $0 init-manager 192.168.1.100

    # 在Worker节点加入Swarm
    $0 init-worker SWMTKN-1-xxx

    # 部署服务（使用默认配置文件）
    $0 deploy

    # 部署服务（使用自定义配置文件）
    $0 deploy docker-compose.custom.yml

    # 扩容Gateway服务到3个副本
    $0 scale ingot_ingot-gateway 3

    # 查看服务状态
    $0 status

    # 查看Gateway日志
    $0 logs ingot-gateway

EOF
}

# 检查是否已初始化Swarm
check_swarm_initialized() {
    if ! docker info | grep -q "Swarm: active"; then
        log_error "Swarm未初始化，请先运行 'init-manager' 或 'init-worker'"
        exit 1
    fi
}

# 初始化Manager节点
init_manager() {
    local advertise_addr=$1

    log_info "初始化Docker Swarm Manager节点..."

    if docker info | grep -q "Swarm: active"; then
        log_warning "Swarm已经初始化"
        return 0
    fi

    if [ -z "$advertise_addr" ]; then
        # 自动获取本机IP
        advertise_addr=$(hostname -I | awk '{print $1}')
        log_info "自动检测到IP地址: $advertise_addr"
    fi

    docker swarm init --advertise-addr "$advertise_addr"

    log_success "Swarm Manager节点初始化完成"
    echo ""
    log_info "请在Worker节点上运行以下命令加入集群："
    echo ""
    docker swarm join-token worker
    echo ""
    log_info "Manager Token (如需添加Manager节点)："
    docker swarm join-token manager
}

# 将节点加入Swarm
init_worker() {
    local join_command=$1

    if [ -z "$join_command" ]; then
        log_error "请提供join token"
        echo "用法: $0 init-worker <join-token>"
        exit 1
    fi

    log_info "将节点加入Swarm集群..."

    if docker info | grep -q "Swarm: active"; then
        log_warning "该节点已经在Swarm集群中"
        return 0
    fi

    # 如果传入的是完整的join命令，直接执行
    if [[ "$join_command" == docker\ swarm\ join* ]]; then
        eval "$join_command"
    else
        log_error "无效的join命令"
        echo "请从Manager节点获取完整的join命令，格式如下："
        echo "docker swarm join --token SWMTKN-1-xxx <manager-ip>:2377"
        exit 1
    fi

    log_success "节点已成功加入Swarm集群"
}

# 部署服务栈
deploy_stack() {
    local compose_file="${1:-docker-compose.yml}"

    check_swarm_initialized

    # 加载 .env 文件
    load_env_file

    log_info "开始部署Ingot Cloud服务栈..."

    # 检查配置文件是否存在
    if [ ! -f "$compose_file" ]; then
        log_error "配置文件 $compose_file 不存在"
        exit 1
    fi

    log_info "使用配置文件: $compose_file"

    # 检查网络是否存在，不存在则创建
    if ! docker network ls | grep -q "ingot-overlay"; then
        log_info "创建 overlay 网络: ingot-overlay"
        docker network create --driver overlay --attachable ingot-overlay || true
    fi

    # 显示当前配置
    log_info "当前配置："
    echo "  - 镜像仓库: ${REGISTRY_URL:-docker-registry.ingotcloud.top}"
    echo "  - 版本: ${VERSION:-0.1.0}"
    echo "  - Gateway副本: ${GATEWAY_REPLICAS:-1}"
    echo "  - Auth副本: ${AUTH_REPLICAS:-1}"
    echo "  - Member副本: ${MEMBER_REPLICAS:-1}"
    echo "  - PMS副本: ${PMS_REPLICAS:-1}"

    # 使用 docker stack deploy 部署
    log_info "部署服务栈: ingot"
    docker stack deploy -c "$compose_file" ingot

    log_success "服务栈部署完成"
    echo ""
    log_info "查看服务状态："
    echo "    docker stack services ingot"
    echo ""
    log_info "查看服务日志："
    echo "    docker service logs -f ingot_ingot-gateway"
}

# 更新服务（滚动更新）
update_services() {
    check_swarm_initialized

    log_info "开始滚动更新服务..."

    # 更新所有服务
    services=$(docker stack services ingot --format "{{.Name}}")

    for service in $services; do
        log_info "更新服务: $service"
        docker service update --image $(docker service inspect --format '{{.Spec.TaskTemplate.ContainerSpec.Image}}' $service) $service
    done

    log_success "服务更新完成"
}

# 扩缩容服务
scale_service() {
    local service_name=$1
    local replicas=$2

    if [ -z "$service_name" ] || [ -z "$replicas" ]; then
        log_error "请指定服务名和副本数"
        echo "用法: $0 scale <service> <replicas>"
        exit 1
    fi

    check_swarm_initialized

    # 如果服务名没有前缀，自动添加
    if [[ ! "$service_name" == ingot_* ]]; then
        service_name="ingot_$service_name"
    fi

    log_info "扩缩容服务 $service_name 到 $replicas 个副本..."
    docker service scale "$service_name=$replicas"

    log_success "扩缩容完成"
}

# 查看服务状态
show_status() {
    check_swarm_initialized

    log_info "Ingot Cloud 服务状态"
    echo ""
    docker stack services ingot
    echo ""
    log_info "服务详细信息"
    echo ""
    docker stack ps ingot --no-trunc
}

# 查看服务日志
show_logs() {
    local service_name=$1

    if [ -z "$service_name" ]; then
        log_error "请指定服务名"
        echo "用法: $0 logs <service>"
        exit 1
    fi

    check_swarm_initialized

    # 如果服务名没有前缀，自动添加
    if [[ ! "$service_name" == ingot_* ]]; then
        service_name="ingot_$service_name"
    fi

    log_info "查看服务日志: $service_name"
    docker service logs -f --tail 100 "$service_name"
}

# 停止并移除服务栈
stop_stack() {
    check_swarm_initialized

    log_warning "即将停止并移除Ingot Cloud服务栈"
    read -p "确认继续? (y/N) " -n 1 -r
    echo

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "操作已取消"
        exit 0
    fi

    log_info "停止服务栈..."
    docker stack rm ingot

    log_success "服务栈已移除"
}

# 离开Swarm集群
leave_swarm() {
    if ! docker info | grep -q "Swarm: active"; then
        log_warning "节点不在Swarm集群中"
        exit 0
    fi

    log_warning "即将离开Swarm集群"
    read -p "确认继续? (y/N) " -n 1 -r
    echo

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "操作已取消"
        exit 0
    fi

    log_info "离开Swarm集群..."
    docker swarm leave --force

    log_success "已离开Swarm集群"
}

# 主函数
main() {
    local command=${1:-help}

    case "$command" in
        init-manager)
            init_manager "$2"
            ;;
        init-worker)
            shift
            init_worker "$*"
            ;;
        deploy)
            deploy_stack "$2"
            ;;
        update)
            update_services
            ;;
        scale)
            scale_service "$2" "$3"
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs "$2"
            ;;
        stop)
            stop_stack
            ;;
        leave)
            leave_swarm
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

