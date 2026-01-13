# OSS 对象存储快速开始

## 5 分钟快速上手

### 1. 添加依赖

在你的服务模块 `build.gradle` 中添加依赖（二选一）：

```gradle
dependencies {
    // 使用 MinIO
    implementation project(':ingot-framework:ingot-oss-minio')
    
    // 或使用 RustFS
    // implementation project(':ingot-framework:ingot-oss-rustfs')
}
```

### 2. 添加配置

在 `application.yml` 中添加配置：

**MinIO：**
```yaml
ingot:
  oss:
    minio:
      url: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
      expired-time: 300
```

**RustFS：**
```yaml
ingot:
  oss:
    rustfs:
      url: http://localhost:8080
      access-key: your-access-key
      secret-key: your-secret-key
      region: us-east-1
      expired-time: 300
```

### 3. 编写代码

```java
package com.your.package.service;

import com.ingot.framework.commons.oss.OSSResult;
import com.ingot.framework.commons.oss.OssService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class FileService {
    private final OssService ossService;

    /**
     * 上传文件
     */
    public OSSResult uploadFile(MultipartFile file) throws IOException {
        String bucket = "my-bucket";
        String fileName = System.currentTimeMillis() + "-" + file.getOriginalFilename();
        return ossService.uploadFile(bucket, fileName, file.getInputStream());
    }

    /**
     * 获取文件访问 URL
     */
    public String getFileUrl(String path) {
        return ossService.getObjectURL(path);
    }
}
```

### 4. 创建 Controller

```java
package com.your.package.controller;

import com.ingot.framework.core.model.common.ApiResult;
import com.ingot.framework.commons.oss.OSSResult;
import com.your.package.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @PostMapping("/upload")
    public ApiResult<OSSResult> upload(@RequestParam("file") MultipartFile file) {
        try {
            OSSResult result = fileService.uploadFile(file);
            return ApiResult.ok(result);
        } catch (Exception e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @GetMapping("/url")
    public ApiResult<String> getUrl(@RequestParam("path") String path) {
        String url = fileService.getFileUrl(path);
        return ApiResult.ok(url);
    }
}
```

### 5. 测试

启动应用后，使用 curl 或 Postman 测试：

```bash
# 上传文件
curl -X POST -F "file=@test.jpg" http://localhost:8080/api/files/upload

# 获取文件 URL
curl "http://localhost:8080/api/files/url?path=my-bucket/1234567890-test.jpg"
```

## 完成！

现在你已经成功集成了对象存储服务！

## 进阶使用

### 在实体类中使用注解

```java
@Data
public class User {
    private Long id;
    private String username;
    
    /**
     * 头像路径（数据库存储 bucket/file.jpg）
     * 查询时自动转换为带签名的 URL
     */
    @OssUrl
    private String avatar;
}
```

### 自定义过期时间

```java
@Data
public class Document {
    /**
     * 文件路径（5分钟过期）
     */
    @OssUrl(expireSeconds = 300)
    private String filePath;
    
    /**
     * 临时文件（1分钟过期）
     */
    @OssUrl(expireSeconds = 60)
    private String tempPath;
}
```

### 保存时自动处理

```java
@Data
public class FileEntity {
    /**
     * 保存时自动去除 URL 签名参数
     * 只保存 bucket/file.jpg 格式
     */
    @OssSaveUrl
    private String filePath;
}
```

## 切换存储

从 MinIO 切换到 RustFS 只需两步：

**步骤 1：** 修改 `build.gradle`
```gradle
dependencies {
    // implementation project(':ingot-framework:ingot-oss-minio')
    implementation project(':ingot-framework:ingot-oss-rustfs')
}
```

**步骤 2：** 修改 `application.yml`
```yaml
# ingot.oss.minio: ...
ingot:
  oss:
    rustfs:
      url: http://localhost:8080
      access-key: your-access-key
      secret-key: your-secret-key
```

业务代码完全不用改！✅

## 更多资源

- [完整架构设计](../../docs/oss-architecture.md)
- [详细使用示例](../../docs/oss-usage-example.md)
- [迁移指南](../../docs/oss-migration-guide.md)
- [重构总结](../../docs/oss-refactoring-summary.md)

## 常用 API

### OssService（业务层）

```java
// 上传文件
OSSResult uploadFile(String bucket, String fileName, InputStream inputStream)

// 下载文件
void getFile(String bucket, String fileName, HttpServletResponse response)

// 获取访问 URL（默认过期时间）
String getObjectURL(String url)

// 获取访问 URL（自定义过期时间）
String getObjectURL(String url, int expiredSeconds)
```

### S3Client（底层）

```java
// 创建 Bucket
void createBucket(String bucketName)

// 检查 Bucket 是否存在
boolean bucketExists(String bucketName)

// 删除 Bucket
void removeBucket(String bucketName)

// 上传对象
void putObject(String bucketName, String objectName, InputStream stream)

// 获取对象
InputStream getObject(String bucketName, String objectName)

// 获取预签名 URL
String getPresignedObjectUrl(String bucketName, String objectName, int duration, TimeUnit unit)

// 删除对象
void removeObject(String bucketName, String objectName)
```

## 最佳实践

1. **数据库存储路径格式：** `bucket/path/to/file.ext`
2. **不要存储完整 URL：** 使用 `@OssUrl` 注解自动转换
3. **合理设置过期时间：**
   - 公开资源：长过期时间（如 1 天）
   - 私密文件：短过期时间（如 5 分钟）
4. **Bucket 命名规范：** 按业务模块划分（avatars、documents、images 等）
5. **文件路径规范：** `{业务类型}/{业务ID}/{时间戳}.{扩展名}`

## 获取帮助

如有问题，请查看：
- [FAQ](../../docs/oss-architecture.md#故障排查)
- 项目文档
- 或联系开发团队
