# OSS
OSS公共模块，包括相关公共内容的定义

### @OssUrl
用于给前端返回临时可访问的OSS地址，可以在注解中自定义过期时间，如果没有定义那么默认使用配置，以minio配置为例`ingot.oss.minio.expiredTime`

### @OssSaveUrl
用于解析保存路径，确保入库内容为`bucket/objectName`，比如URL为https://host:9000/bucket/dir/file.png?a=2&b=3，那么最终入库的内容为`bucket/dir/file.png`