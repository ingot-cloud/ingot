# Ingot OSS RustFS

## 概述

`ingot-oss-rustfs` 是基于 AWS S3 SDK 实现的 RustFS 对象存储客户端，提供了与 MinIO 相同的接口，支持无缝切换。

## 依赖

```gradle
dependencies {
    implementation project(":ingot-framework:ingot-oss-rustfs")
}
```

## 配置

```yaml
ingot:
  oss:
    rustfs:
      # RustFS 服务地址
      url: http://localhost:8080
      # Access Key
      access-key: your-access-key
      # Secret Key
      secret-key: your-secret-key
      # 区域，默认 us-east-1
      region: us-east-1
      # 默认过期时间（秒），默认 300 秒
      expired-time: 300
      # 端点配置（可选）
      endpoint:
        enable: false
        name: oss
```

## 特性

- 基于 AWS S3 SDK 实现
- 完全兼容 S3 协议
- 支持预签名 URL
- 自动初始化客户端
- 统一的异常处理

## 使用示例

### 注入服务

```java
@Service
@RequiredArgsConstructor
public class FileService {
    private final OssService ossService;

    public OSSResult uploadFile(MultipartFile file) throws IOException {
        String bucket = "my-bucket";
        String fileName = "path/to/" + file.getOriginalFilename();
        return ossService.uploadFile(bucket, fileName, file.getInputStream());
    }

    public String getFileUrl(String path) {
        return ossService.getObjectURL(path);
    }

    public String getFileUrl(String path, int expireSeconds) {
        return ossService.getObjectURL(path, expireSeconds);
    }
}
```

### 直接使用 RustfsService

如果需要更底层的控制，可以直接注入 `RustfsService`：

```java
@Service
@RequiredArgsConstructor
public class AdvancedFileService {
    private final RustfsService rustfsService;

    public void createBucket(String bucketName) {
        rustfsService.createBucket(bucketName);
    }

    public boolean checkBucket(String bucketName) {
        return rustfsService.bucketExists(bucketName);
    }

    public void deleteBucket(String bucketName) {
        rustfsService.removeBucket(bucketName);
    }
}
```

## 从 MinIO 切换到 RustFS

1. 修改 Gradle 依赖：

```gradle
// 移除 MinIO 依赖
// implementation project(":ingot-framework:ingot-oss-minio")

// 添加 RustFS 依赖
implementation project(":ingot-framework:ingot-oss-rustfs")
```

2. 修改配置文件：

```yaml
# 注释或删除 MinIO 配置
# ingot:
#   oss:
#     minio:
#       url: http://localhost:9000
#       access-key: minioadmin
#       secret-key: minioadmin

# 添加 RustFS 配置
ingot:
  oss:
    rustfs:
      url: http://localhost:8080
      access-key: your-access-key
      secret-key: your-secret-key
      region: us-east-1
```

3. 业务代码无需修改！

## 技术实现

- **AWS S3 SDK** - 使用官方的 AWS S3 Java SDK
- **S3Client** - 实现了 `com.ingot.framework.oss.common.S3Client` 接口
- **S3Presigner** - 用于生成预签名 URL
- **自动配置** - 通过 `RustfsAutoConfiguration` 自动配置 Bean

## 注意事项

1. **区域设置** - RustFS 需要指定区域，默认为 `us-east-1`
2. **端点覆盖** - 使用 `endpointOverride` 来指定自定义的服务端点
3. **凭证配置** - 使用静态凭证提供者 `StaticCredentialsProvider`
4. **异常处理** - 所有异常都会被包装为 `RuntimeException` 或 `Exception`

## 相关链接

- [AWS S3 SDK 文档](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
- [S3 API 参考](https://docs.aws.amazon.com/AmazonS3/latest/API/Welcome.html)
