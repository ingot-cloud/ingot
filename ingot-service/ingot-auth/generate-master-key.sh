#!/bin/bash

###############################################################################
# JWK Master Key 生成脚本
# 用于生成用于加密 Redis 中私钥的主密钥
###############################################################################

set -e

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║          JWK Master Key 生成工具                              ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# 检查 openssl 是否安装
if ! command -v openssl &> /dev/null; then
    echo "❌ 错误：未找到 openssl 命令"
    echo "请先安装 openssl："
    echo "  - macOS: brew install openssl"
    echo "  - Ubuntu/Debian: apt-get install openssl"
    echo "  - CentOS/RHEL: yum install openssl"
    exit 1
fi

echo "📝 正在生成随机主密钥..."
echo ""

# 生成 256 位（32 字节）随机密钥，Base64 编码
MASTER_KEY=$(openssl rand -base64 32)

echo "✅ 主密钥生成成功！"
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  请妥善保存以下主密钥，它将用于加密 Redis 中的私钥          ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "Master Key:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "$MASTER_KEY"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 提供不同场景的使用示例
echo "📖 使用方法："
echo ""
echo "1️⃣  设置环境变量（推荐）："
echo "   export AUTH_JWK_MASTER_KEY=\"$MASTER_KEY\""
echo ""
echo "2️⃣  添加到配置文件："
echo "   ingot:"
echo "     security:"
echo "       jwk:"
echo "         master-key: \${AUTH_JWK_MASTER_KEY}"
echo ""
echo "3️⃣  Docker 部署："
echo "   docker run -e AUTH_JWK_MASTER_KEY=\"$MASTER_KEY\" ..."
echo ""
echo "4️⃣  Kubernetes Secret："
echo "   kubectl create secret generic ingot-jwk-secret \\"
echo "     --from-literal=master-key='$MASTER_KEY'"
echo ""

# 询问是否创建 .env 文件
echo ""
read -p "❓ 是否创建 .env 文件（用于本地开发）？[y/N] " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    ENV_FILE=".env"
    
    if [ -f "$ENV_FILE" ]; then
        echo "⚠️  警告：$ENV_FILE 已存在"
        read -p "是否覆盖？[y/N] " -n 1 -r
        echo ""
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "❌ 取消操作"
            exit 0
        fi
    fi
    
    cat > "$ENV_FILE" << EOF
# JWK Master Key (自动生成 - $(date))
# ⚠️ 重要：请勿将此文件提交到版本控制系统！
AUTH_JWK_MASTER_KEY=$MASTER_KEY

# Redis 配置（根据实际情况修改）
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
EOF
    
    echo "✅ 已创建 $ENV_FILE 文件"
    echo ""
    echo "使用方法："
    echo "  source .env"
    echo "  java -jar ingot-auth.jar"
    echo ""
    
    # 添加到 .gitignore
    if [ -f ".gitignore" ]; then
        if ! grep -q "^\.env$" .gitignore; then
            echo ".env" >> .gitignore
            echo "✅ 已将 .env 添加到 .gitignore"
        fi
    else
        echo ".env" > .gitignore
        echo "✅ 已创建 .gitignore 并添加 .env"
    fi
fi

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  ⚠️  安全提醒                                                 ║"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║  1. 请务必备份此主密钥到安全位置                                  ║"
echo "║  2. 不同环境使用不同的主密钥                                     ║"
echo "║  3. 定期轮换主密钥（需要重新加密所有密钥）                         ║"
echo "║  4. 不要将主密钥硬编码在代码或配置文件中                           ║"
echo "║  5. 不要将 .env 文件提交到版本控制系统                            ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "✨ 完成！现在可以启动授权服务器了。"
echo ""

