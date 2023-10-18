## OAuth2

### 1.密码授权
```
url: /auth/oauth2/token
method: POST
```
Header
```
Authorization: BasicToken
```
#### 1.password登录
```
user_type: "用户类型"
grant_type: "password"
username: "xxx"
password: "xxx"
_vc_code: "验证码"
org: "要登录的组织ID"
```
#### 2.社交登录
```
user_type: "用户类型"
grant_type: "social"
social_type: "自定义类型"
social_code: "code"
```

### 2.预授权
1.通过访问`auth-service`中`/oauth2/pre_authorize`进行预授权，并且返回可以访问的组织列表
请求参数
```
user_type: "用户类型"
_vc_code: "验证码"
pre_grant_type: "password" // PreAuthorizationGrantType
username: "类型为password时必填"
password: "类型为password时必填"
social_type: "类型为social时必填"
social_code: "类型为social时必填"

client_id: string;
code_challenge: string;
response_type: string;
redirect_uri: string;
scope: string;
state: string;
```
2.选择组织后，执行`/oauth2/authorize`授权码模式请求
请求参数
```
pre_grant_type: "预授权类型，和第一步传值保持一致"
org: "组织ID"

client_id: string;
code_challenge: string;
response_type: string;
redirect_uri: string;
scope: string;
state: string;
```
3.执行`/oauth2/token`，获取授权信息
```
code: "授权码"
grant_type: "authorization_code",
code_verifier: "",
client_id: "",
redirect_uri: ""
```
