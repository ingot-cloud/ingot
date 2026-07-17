# OSS 对象存储使用示例

## 概述

本文档展示如何在 Ingot Cloud 项目中使用对象存储服务，以及如何在不同的 S3 兼容存储（MinIO、RustFS）之间无缝切换。

## 依赖配置

### 使用 MinIO

在你的服务模块的 `build.gradle` 中添加：

```gradle
dependencies {
    implementation project(':ingot-framework:ingot-oss-minio')
}
```

### 使用 RustFS

在你的服务模块的 `build.gradle` 中添加：

```gradle
dependencies {
    implementation project(':ingot-framework:ingot-oss-rustfs')
}
```

> **注意：** 两者选其一即可，业务代码完全兼容。

## 配置文件

### MinIO 配置 (application.yml)

```yaml
ingot:
  oss:
    minio:
      url: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
      expired-time: 300  # URL过期时间（秒）
      endpoint:
        enable: true
        name: oss  # 端点访问路径 /oss/minio
```

### RustFS 配置 (application.yml)

```yaml
ingot:
  oss:
    rustfs:
      url: http://localhost:8080
      access-key: your-access-key
      secret-key: your-secret-key
      region: us-east-1
      expired-time: 300  # URL过期时间（秒）
      endpoint:
        enable: true
        name: oss  # 端点访问路径 /oss/rustfs
```

## 基础使用示例

### 1. 文件上传服务

```java
package com.ingot.cloud.pms.service.impl;

import java.io.IOException;
import java.io.InputStream;

import com.ingot.framework.commons.oss.OSSResult;
import com.ingot.framework.commons.oss.OssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {
    private final OssService ossService;

    /**
     * 上传文件
     */
    public OSSResult uploadFile(String bucket, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            String fileName = generateFileName(file.getOriginalFilename());
            return ossService.uploadFile(bucket, fileName, inputStream);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传用户头像
     */
    public String uploadAvatar(Long userId, MultipartFile file) {
        String bucket = "avatars";
        String fileName = "user/" + userId + "/" + System.currentTimeMillis() + getFileExtension(file);
        
        try (InputStream inputStream = file.getInputStream()) {
            OSSResult result = ossService.uploadFile(bucket, fileName, inputStream);
            // 返回不带签名的路径（存储到数据库）
            return bucket + "/" + fileName;
        } catch (IOException e) {
            log.error("头像上传失败", e);
            throw new RuntimeException("头像上传失败");
        }
    }

    /**
     * 获取文件访问URL
     */
    public String getFileUrl(String path) {
        return ossService.getObjectURL(path);
    }

    /**
     * 获取文件访问URL（自定义过期时间）
     */
    public String getFileUrl(String path, int expireSeconds) {
        return ossService.getObjectURL(path, expireSeconds);
    }

    private String generateFileName(String originalFilename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = getFileExtension(originalFilename);
        return timestamp + extension;
    }

    private String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "";
    }
}
```

### 2. Controller 示例

```java
package com.ingot.cloud.pms.web.v1.admin;

import com.ingot.cloud.pms.service.impl.FileUploadService;
import com.ingot.framework.core.model.common.ApiResult;
import com.ingot.framework.commons.oss.OSSResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件管理")
@RestController
@RequestMapping("/v1/admin/files")
@RequiredArgsConstructor
public class FileController {
    private final FileUploadService fileUploadService;

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public ApiResult<OSSResult> upload(
            @RequestParam("bucket") String bucket,
            @RequestParam("file") MultipartFile file) {
        OSSResult result = fileUploadService.uploadFile(bucket, file);
        return ApiResult.ok(result);
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public ApiResult<String> uploadAvatar(
            @RequestParam("userId") Long userId,
            @RequestParam("file") MultipartFile file) {
        String path = fileUploadService.uploadAvatar(userId, file);
        return ApiResult.ok(path);
    }

    @Operation(summary = "获取文件访问URL")
    @GetMapping("/url")
    public ApiResult<String> getFileUrl(@RequestParam("path") String path) {
        String url = fileUploadService.getFileUrl(path);
        return ApiResult.ok(url);
    }
}
```

## 在实体类中使用注解

### 1. 实体类定义

```java
package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.data.mybatis.model.BaseModel;
import com.ingot.framework.oss.common.OssUrl;
import com.ingot.framework.oss.common.OssSaveUrl;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseModel<SysUser> {
    /**
     * 用户名
     */
    private String username;

    /**
     * 头像路径（数据库存储相对路径）
     * 查询时自动转换为带签名的访问URL
     */
    @OssUrl
    private String avatar;

    /**
     * 背景图（自定义过期时间）
     */
    @OssUrl(expireSeconds = 600)
    private String backgroundImage;

    /**
     * 附件路径
     * 保存时自动去除签名参数
     */
    @OssSaveUrl
    private String attachmentPath;
}
```

### 2. DTO 定义

```java
package com.ingot.cloud.pms.api.model.dto;

import com.ingot.framework.oss.common.OssUrl;
import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String username;

    /**
     * 头像URL（自动转换为预签名URL）
     */
    @OssUrl
    private String avatar;

    /**
     * 背景图URL（10分钟过期）
     */
    @OssUrl(expireSeconds = 600)
    private String backgroundImage;
}
```

## 如何切换存储实现

### 场景：从 MinIO 切换到 RustFS

#### 步骤1：修改依赖

编辑 `build.gradle`：

```gradle
dependencies {
    // 注释或删除 MinIO 依赖
    // implementation project(':ingot-framework:ingot-oss-minio')
    
    // 添加 RustFS 依赖
    implementation project(':ingot-framework:ingot-oss-rustfs')
}
```

#### 步骤2：修改配置

编辑 `application.yml`：

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
      expired-time: 300
```

#### 步骤3：重新编译和启动

```bash
./gradlew clean build
./gradlew :ingot-service:ingot-pms:ingot-pms-provider:bootRun
```

**业务代码无需任何修改！**

## 高级用法

### 1. 直接使用底层客户端

如果需要更底层的控制（如 Bucket 管理），可以直接注入具体的服务：

```java
@Service
@RequiredArgsConstructor
public class BucketManagementService {
    // 使用 MinIO 时
    private final MinioService minioService;
    
    // 或使用 RustFS 时
    // private final RustfsService rustfsService;

    public void initBucket(String bucketName) {
        minioService.createBucket(bucketName);
    }

    public boolean checkBucket(String bucketName) {
        return minioService.bucketExists(bucketName);
    }
}
```

### 2. 使用 S3Client 接口

更好的做法是使用 `S3Client` 接口，这样代码可以兼容所有实现：

```java
@Service
@RequiredArgsConstructor
public class BucketManagementService {
    private final S3Client s3Client;  // 可以是 MinioService 或 RustfsService

    public void initBucket(String bucketName) {
        if (!s3Client.bucketExists(bucketName)) {
            s3Client.createBucket(bucketName);
        }
    }

    public void deleteBucket(String bucketName) {
        s3Client.removeBucket(bucketName);
    }
}
```

## 最佳实践

### 1. 路径存储规范

建议在数据库中存储相对路径，而不是完整URL：

```
✅ 推荐：avatars/user/123/1704096000000.jpg
❌ 不推荐：http://localhost:9000/avatars/user/123/1704096000000.jpg?X-Amz-Algorithm=...
```

优势：
- 数据库存储空间更小
- 迁移存储服务更方便
- URL签名失效不影响数据

### 2. 使用注解自动转换

对于需要返回给前端的路径，使用 `@OssUrl` 注解自动转换：

```java
@Data
public class FileResponse {
    @OssUrl
    private String path;  // 自动转换为带签名的URL
}
```

### 3. Bucket 命名规范

建议按业务模块划分 Bucket：

```
avatars      - 用户头像
documents    - 文档文件
images       - 图片资源
videos       - 视频资源
temp         - 临时文件
```

### 4. 文件路径规范

建议的文件路径结构：

```
{bucket}/{业务类型}/{业务ID}/{时间戳}.{扩展名}

示例：
avatars/user/123/1704096000000.jpg
documents/order/456/contract-1704096000000.pdf
images/product/789/cover-1704096000000.png
```

## 性能优化建议

### 1. 批量URL生成

如果需要生成大量文件的访问URL，考虑缓存或批量处理。

### 2. CDN 集成

生产环境建议配置 CDN，减少直接访问对象存储的压力。

### 3. 过期时间设置

根据业务场景合理设置过期时间：
- 公开资源（Logo等）：长时间（如1天）
- 私密文件：短时间（如5分钟）
- 临时文件：极短时间（如1分钟）

## 故障排查

### 1. 连接失败

检查配置：
```yaml
ingot:
  oss:
    minio:  # 或 rustfs
      url: http://localhost:9000  # 确保地址正确
```

### 2. 认证失败

检查 Access Key 和 Secret Key 是否正确。

### 3. Bucket 不存在

确保 Bucket 已创建，或在代码中自动创建：

```java
if (!s3Client.bucketExists(bucketName)) {
    s3Client.createBucket(bucketName);
}
```

## 总结

通过这个抽象层设计，你可以：

1. ✅ 业务代码与存储实现解耦
2. ✅ 轻松在不同存储之间切换
3. ✅ 统一的接口和使用方式
4. ✅ 自动URL转换和签名处理
5. ✅ 类型安全和编译期检查
