# OAuth2
扩展`OAuth2`授权方式

## 公共说明
接口请求地址都是相对于网关的路径，网关`BaseURL`为`http://localhost:7980`

 * 用户类型

    | text  | value |
    |-------| --- | 
    | 管理用户  | 0 |
    | App用户 | 1 |

 * 社交类型

    | text       | value |
    |------------|-------|
    | 系统后台-短信登录  | admin_sms |
    | 系统后台-微信登录  | admin_wechat |
    | 系统后台-微信小程序 | admin_miniprogram |
    | App-短信登录   | app_sms |
    | App-微信登录       | app_wechat |
    | App-微信小程序      | app_miniprogram |

 * 预授权类型

   | text | value |
   |------| --- | 
   | 密码模式 | password |
   | 社交模式 | social |

## 1.自定义授权
自定义授权核心类`OAuth2CustomAuthenticationProvider`，具体包括密码模式和社交模式

### 密码模式

 - URL：`{BaseURL}/auth/oauth2/token`
 - Method：`POST`
 - Header：
    ```json
      {
        "Authorization": "BasicToken"
      }
    ```
 - 请求参数：`x-www-form-urlencoded`
   - grant_type: 授权类型
   - username: 用户名
   - password: 密码
   - user_type: 用户类型
   - org: 组织ID
 - 响应参数：`json`
   - expiresIn: 过期时间
   - org: 组织ID
   - scope: 权限范围
   - accessToken: Token
   - tokenType: Token类型
   - refreshToken: 刷新Token，如果指定客户端没有配置那么为空
 - 请求示例:
    ```
      grant_type: "password"
      username: "username"
      password: "password"
      org: "1"
      user_type: "0"
      _vc_code: "验证码"
    ```
 - 响应示例：
   ```json
    {
      "code": "S0200",
      "data": {
          "expiresIn": "7199",
          "org": "1",
          "scope": "message.read message.write",
          "accessToken": "",
          "tokenType": "Bearer",
          "refreshToken": ""
      }
    }
   ```

### 社交登录

- URL：`{BaseURL}/auth/oauth2/token`
- Method：`POST`
- Header：
   ```json
     {
       "Authorization": "BasicToken"
     }
   ```
- 请求参数：`x-www-form-urlencoded`
    - grant_type: 授权类型
    - social_type: 社交类型
    - social_code: 社交编码，比如微信小程序的`login code`
    - user_type: 用户类型
    - org: 组织ID
- 响应参数：`json`
    - expiresIn: 过期时间
    - org: 组织ID
    - scope: 权限范围
    - accessToken: Token
    - tokenType: Token类型
    - refreshToken: 刷新Token，如果指定客户端没有配置那么为空
- 请求示例:
   ```
     grant_type: "social"
     social_type: "app_miniprogram"
     social_code: "password"
     user_type: "1"
     org: "1"
   ```
- 响应示例：
  ```json
   {
     "code": "S0200",
     "data": {
         "expiresIn": "7199",
         "org": "1",
         "scope": "message.read message.write",
         "accessToken": "",
         "tokenType": "Bearer",
         "refreshToken": ""
     }
   }
  ```

## 2.预授权
预授权流程如下：
 1. 用户进行预授权认证，密码模式或者社交模式
 2. 预授权认证成功，返回可以登录的组织，选择具体组织后获取授权码
 3. 通过授权码进行授权码登录 

#### 预授权接口
 - URL：`{BaseURL}/auth/oauth2/pre_authorize`
 - Method：`POST`
 - 请求参数：`Query Params` + `x-www-form-urlencoded`
   - user_type：用户类型
   - pre_grant_type：预授权类型
   - client_id：客户端ID
   - code_challenge：PKCE challenge
   - response_type：响应类型
   - redirect_uri：重定向地址
   - scope：权限范围
   - state：校验状态
   - username：用户名，预授权类型为password时必填(x-www-form-urlencoded)
   - password：密码，预授权类型为password时必填(x-www-form-urlencoded)
   - social_type：授权类型，预授权类型为social时必填(x-www-form-urlencoded)
   - social_code：授权编码，预授权类型为social时必填(x-www-form-urlencoded)
   - _vc_code：验证码
 - 响应参数：`json`
   - allows：允许访问的组织，`Array`
     - id：组织ID
     - name：组织名字
     - avatar：组织logo
 - 请求示例：
   ```
   http://localhost:1798/api/auth/oauth2/pre_authorize?
   user_type=0
   &_vc_code=xhaNas%2BNCIU%3D
   &pre_grant_type=password
   &client_id=ingot
   &code_challenge=zhERTxts
   &response_type=code
   &redirect_uri=http:%2F%2Flocalhost:5798%2Fsso_callback
   &scope=message.write
   &state=1csHlU
   
   # x-www-form-urlencoded
   username: ****
   password: ****
   ```
 - 响应示例：
    ```json
   {
    "code": "S0200",
    "data": {
        "allows": [
            {
                "id": "1",
                "name": "组织名称",
                "avatar": "url",
                "main": true
            }
        ]
    }
   }
    ```

#### 授权码请求

 - URL：`{BaseURL}/auth/oauth2/authorize`
 - Method：`GET`
 - 请求参数：`Query Params`
   - pre_grant_type：预授权类型，和预授权接口保持一致
   - org：组织ID
   - client_id：客户端ID
   - code_challenge：PKCE challenge
   - response_type：响应类型
   - redirect_uri：重定向地址
   - scope：权限范围
   - state：校验状态
 - 响应参数：`json`
   - code：授权码
   - state：校验状态
   - redirect_uri：重定向URL
 - 请求示例：
    ```
   http://localhost:1798/api/auth/oauth2/authorize?
   pre_grant_type=password
   &org=0
   &client_id=ingot
   &code_challenge=zhERTxts
   &response_type=code
   &redirect_uri=http:%2F%2Flocalhost:5798%2Fsso_callback
   &scope=message.write
   &state=1csHlU
    ```
 - 响应示例：
    ```json
   {
    "code": "S0200",
    "data": {
        "code": "asdfasfa",
        "state": "asasda",
        "redirect_uri": "http://asdas/com"
    }
   }
    ```

#### 授权码获取token

 - URL：`{BaseURL}/auth/oauth2/token`
 - Method：`POST`
 - 请求参数：`Form Data`
   - code：授权码
   - grant_type：授权类型authorization_code
   - code_verifier：PKCE verifier
   - client_id：客户端ID
   - redirect_uri：重定向URL
 - 响应参数：`json`
   - expiresIn: 过期时间
   - org: 组织ID
   - scope: 权限范围
   - accessToken: Token
   - tokenType: Token类型
 - 请求示例:
   ```
     code: "aaadasf"
     grant_type: "authorization_code"
     code_verifier: "asasdasf"
     client_id: "1"
     redirect_uri: "http://aaaa"
   ```
 - 响应示例：
  ```json
   {
     "code": "S0200",
     "data": {
         "expiresIn": "7199",
         "org": "1",
         "scope": "message.read message.write",
         "accessToken": "",
         "tokenType": "Bearer",
         "refreshToken": ""
     }
   }
  ```
