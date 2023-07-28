## OAuth2

### 登录
```
url: /auth/oauth2/token
method: POST
```
Header
```
Authorization: BasicToken
Tenant: 租户ID
```
#### 1.password登录
参数
```
grant_type: "password"
username: "xxx"
password: "xxx"
_vc_code: "验证码"
```

#### 2.社交登录
```
grant_type: "social"
social_type: "自定义类型"
social_code: "code"
```

### 租户选择登录

1.通过访问`auth-service`中`/oauth2/pre_authorize`端点登录获取预授权code和租户列表
```
pre_authorization: "password_code" // OAuth2PreAuthorizationType
username: "类型为password_code时必填"
password: "类型为password_code时必填"
social_type: "类型为social_code时必填"
social_code: "类型为social_code时必填"
```
2.选择租户后，通过code进行自定义社交登录