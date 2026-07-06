# Crypto加密模块

## 使用方法
 - InFieldEncrypt: 加密注解，用于给前端返回数据时候进行加密处理
 - InFieldDecrypt: 解密注解，用于解密前端传递过来的加密数据
```java
    @Data
    public static class TestCrypto {
        @InFieldEncrypt(CryptoType.AES)
        @InFieldDecrypt(CryptoType.AES)
        private String cbc;
        @InFieldEncrypt(CryptoType.AES_GCM)
        @InFieldDecrypt(CryptoType.AES_GCM)
        private String gcm;
    }
```