# Core Network — Pixiv API & Network Infrastructure

独立的 Pixiv 网络传输与协议层 Android Library 模块，提供开箱即用的 Pixiv 官方 API、OAuth 认证管理、DoH 防阻断与 C++ HMAC 原生签名。

## 模块定位

- **完全解耦 UI 与持久化**：不依赖 `Activity`、`Fragment` 或具体的本地数据库/SharedPreferences 实现。
- **协议契约（Contracts）**：
  - `TokenProvider`：账号令牌获取与单飞（SingleFlight）防并发冲突自动刷新。
  - `NetworkConfigProvider`：反向代理（PxveAPI 协议）、直连与 DoH 控制。
  - `LanguageProvider`：多语言与区域 Accept-Language 动态注入。
  - `CookieProvider`：Web 与 Fanbox 会话 Cookie 支持。
  - `NetworkMode`：三档网络连接模式（Standard、DirectCompat、DirectEch、CustomProxy）。
- **原生安全构建**：包含自包含的 CMake C++20 原生库（`libshaft_secrets.so`，符合 16KB 内存对齐标准），构建期生成掩码头文件，防止明文密钥泄露。

## 架构组成

```
ceui.pixiv.network
├── contract/             # 解耦契约 (TokenProvider, NetworkConfigProvider 等)
├── api/                  # Retrofit 接口 (API.kt, PixivWebApi.kt, ComicApi.kt, FanboxApi.kt)
│   ├── model/            # 接口专属数据模型 (Illust, Novel, User 等)
│   ├── PixivClient.kt    # 全局访问外观
│   └── PixivClientManager# 独立服务构建工厂
├── session/              # SingleFlightTokenRefresher 并发刷新协调器
├── shaftapi/             # ShaftHmac 原生 JNI 桥接
└── http/                 # 传输层拦截器链与网络工具 (Cronet, HttpDns, IPv4OnlyDns 等)
```

## 接入方式

### 1. 声明依赖
在宿主应用（如 `:app` 或外部 `:Pixiv-Shaft-pin`）的 `build.gradle` 中添加：

```groovy
dependencies {
    implementation project(':core-network')
}
```

### 2. 注入契约配置
```kotlin
PixivClient.manager = PixivClientManager(
    tokenProvider = myTokenProvider,
    configProvider = myConfigProvider,
    languageProvider = myLanguageProvider,
    cookieProvider = myCookieProvider,
    cronetEngineSupplier = { myCronetEngine }
)
```

### 3. 发起请求
```kotlin
val illusts = PixivClient.appApi.getRecommended("illust")
```
