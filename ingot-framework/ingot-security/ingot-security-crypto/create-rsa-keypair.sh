# 1. 生成 2048 位私钥
openssl genrsa -out hybrid-private.pem 2048

# 2. 导出公钥
openssl rsa -in hybrid-private.pem -pubout -out hybrid-public.pem

# 3. 转成配置所需的 Base64（单行，无换行）
# 公钥 X509 DER Base64
openssl rsa -in hybrid-public.pem -pubin -outform DER | base64 | tr -d '\n'

# 私钥 PKCS8 DER Base64
openssl pkcs8 -topk8 -inform PEM -outform DER -in hybrid-private.pem -nocrypt | base64 | tr -d '\n'