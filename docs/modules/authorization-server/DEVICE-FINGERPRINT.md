# 前端设备指纹集成指南

## 概述

BFF 会话安全机制要求前端在每次 HTTP 请求中携带设备指纹 Header `In-Ca-Sig`，用于绑定 Cookie Session 与物理设备，防止 Cookie 被窃取后在其他设备上使用。

**安全模型：**
- HTTPS 防止中间人看到请求内容（包括 Header 和 Cookie）
- HttpOnly Cookie 防止 XSS 读取 Cookie
- 设备指纹防止被窃取的 Cookie 在其他设备上使用（攻击者的设备特征不同，无法生成相同指纹）

## 指纹计算代码

### 依赖安装

```bash
npm install crypto-js
npm install -D @types/crypto-js  # TypeScript 项目需要
```

> `crypto.subtle` API 只能在 `localhost` 或 HTTPS 环境下使用，
> 为了兼容 HTTP 开发/测试环境，统一使用 `crypto-js` 进行 SHA-256 计算。

### TypeScript 实现

```typescript
/**
 * 设备指纹生成器
 *
 * 通过采集浏览器多维度特征并计算 SHA-256 摘要，生成稳定的设备标识。
 * 同一设备+浏览器的指纹在多次访问中保持一致，不同设备的指纹不同。
 *
 * 采集维度：
 * - 基础：User-Agent、语言、平台、CPU核心数、内存、时区
 * - 屏幕：分辨率、色深、像素比
 * - Canvas 2D：不同 GPU/驱动/字体渲染结果不同
 * - WebGL：GPU 厂商和渲染器信息
 * - 音频：AudioContext 处理差异
 */
import SHA256 from 'crypto-js/sha256';
import Hex from 'crypto-js/enc-hex';

interface FingerprintComponent {
  key: string;
  value: string;
}

/**
 * 生成设备指纹（SHA-256 hex）
 *
 * @returns 64 字符的十六进制字符串
 *
 * @example
 * ```ts
 * const fp = await generateFingerprint();
 * // "3a7bd3e2...64位hex..."
 * ```
 */
export async function generateFingerprint(): Promise<string> {
  const components = await collectComponents();
  const raw = components.map((c) => `${c.key}:${c.value}`).join('|');
  return SHA256(raw).toString(Hex);
}

async function collectComponents(): Promise<FingerprintComponent[]> {
  const components: FingerprintComponent[] = [];

  // ---- 基础环境 ----
  components.push({ key: 'ua', value: navigator.userAgent });
  components.push({ key: 'lang', value: navigator.language });
  components.push({ key: 'langs', value: (navigator.languages || []).join(',') });
  components.push({ key: 'platform', value: navigator.platform });
  components.push({ key: 'cores', value: String(navigator.hardwareConcurrency || 0) });
  components.push({ key: 'mem', value: String((navigator as any).deviceMemory || 0) });
  components.push({ key: 'tz', value: Intl.DateTimeFormat().resolvedOptions().timeZone });
  components.push({ key: 'tzo', value: String(new Date().getTimezoneOffset()) });
  components.push({ key: 'cookie', value: String(navigator.cookieEnabled) });
  components.push({ key: 'dnt', value: String(navigator.doNotTrack || '') });

  // ---- 屏幕特征 ----
  components.push({ key: 'res', value: `${screen.width}x${screen.height}` });
  components.push({ key: 'avail', value: `${screen.availWidth}x${screen.availHeight}` });
  components.push({ key: 'depth', value: String(screen.colorDepth) });
  components.push({ key: 'dpr', value: String(window.devicePixelRatio || 1) });

  // ---- Canvas 指纹 ----
  components.push({ key: 'canvas', value: getCanvasFingerprint() });

  // ---- WebGL 指纹 ----
  components.push({ key: 'webgl', value: getWebGLFingerprint() });

  // ---- Audio 指纹 ----
  const audioFp = await getAudioFingerprint();
  components.push({ key: 'audio', value: audioFp });

  return components;
}

/**
 * Canvas 2D 指纹
 *
 * 绘制特定图形和文字，不同设备的 GPU、字体渲染引擎、抗锯齿算法
 * 会产生像素级差异，导致 toDataURL() 输出不同。
 */
function getCanvasFingerprint(): string {
  try {
    const canvas = document.createElement('canvas');
    canvas.width = 200;
    canvas.height = 50;
    const ctx = canvas.getContext('2d');
    if (!ctx) return '';

    const gradient = ctx.createLinearGradient(0, 0, 200, 0);
    gradient.addColorStop(0, '#ff6b6b');
    gradient.addColorStop(1, '#4ecdc4');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, 200, 50);

    ctx.fillStyle = '#2d3436';
    ctx.font = '18px Arial, sans-serif';
    ctx.textBaseline = 'top';
    ctx.fillText('Ingot.FP.2024', 2, 2);

    ctx.fillStyle = 'rgba(102, 204, 170, 0.7)';
    ctx.beginPath();
    ctx.arc(150, 25, 20, 0, Math.PI * 2);
    ctx.fill();

    return canvas.toDataURL();
  } catch {
    return '';
  }
}

/**
 * WebGL 指纹
 *
 * 获取 GPU 的 VENDOR 和 RENDERER 信息，
 * 这在不同硬件设备上几乎唯一。
 */
function getWebGLFingerprint(): string {
  try {
    const canvas = document.createElement('canvas');
    const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
    if (!gl || !(gl instanceof WebGLRenderingContext)) return '';

    const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
    if (!debugInfo) return '';

    const vendor = gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL) || '';
    const renderer = gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL) || '';
    return `${vendor}~${renderer}`;
  } catch {
    return '';
  }
}

/**
 * Audio 指纹
 *
 * 通过 OfflineAudioContext 生成音频采样，
 * 不同音频处理芯片和驱动会产生微小的浮点差异。
 */
function getAudioFingerprint(): Promise<string> {
  return new Promise((resolve) => {
    try {
      const AudioCtx = window.OfflineAudioContext || (window as any).webkitOfflineAudioContext;
      if (!AudioCtx) {
        resolve('');
        return;
      }

      const context = new AudioCtx(1, 44100, 44100);
      const oscillator = context.createOscillator();
      oscillator.type = 'triangle';
      oscillator.frequency.setValueAtTime(10000, context.currentTime);

      const compressor = context.createDynamicsCompressor();
      compressor.threshold.setValueAtTime(-50, context.currentTime);
      compressor.knee.setValueAtTime(40, context.currentTime);
      compressor.ratio.setValueAtTime(12, context.currentTime);
      compressor.attack.setValueAtTime(0, context.currentTime);
      compressor.release.setValueAtTime(0.25, context.currentTime);

      oscillator.connect(compressor);
      compressor.connect(context.destination);
      oscillator.start(0);

      context.startRendering().then((buffer) => {
        const data = buffer.getChannelData(0);
        let sum = 0;
        for (let i = 4500; i < 5000; i++) {
          sum += Math.abs(data[i]);
        }
        resolve(sum.toFixed(6));
      }).catch(() => resolve(''));

      setTimeout(() => resolve(''), 1000);
    } catch {
      resolve('');
    }
  });
}
```

## 集成方式

### 方式一：Axios 拦截器（推荐）

```typescript
import axios from 'axios';
import { generateFingerprint } from '@/utils/fingerprint';

let cachedFingerprint: string | null = null;

axios.interceptors.request.use(async (config) => {
  if (!cachedFingerprint) {
    cachedFingerprint = await generateFingerprint();
  }
  config.headers['In-Ca-Sig'] = cachedFingerprint;
  return config;
});
```

### 方式二：Fetch 封装

```typescript
import { generateFingerprint } from '@/utils/fingerprint';

let cachedFingerprint: string | null = null;

export async function request(url: string, options: RequestInit = {}) {
  if (!cachedFingerprint) {
    cachedFingerprint = await generateFingerprint();
  }
  
  const headers = new Headers(options.headers);
  headers.set('In-Ca-Sig', cachedFingerprint);
  
  return fetch(url, { ...options, headers });
}
```

### 方式三：umi-request / @umijs/max

```typescript
import { generateFingerprint } from '@/utils/fingerprint';

let cachedFingerprint: string | null = null;

export const requestInterceptor = async (url: string, options: any) => {
  if (!cachedFingerprint) {
    cachedFingerprint = await generateFingerprint();
  }
  return {
    url,
    options: {
      ...options,
      headers: {
        ...options.headers,
        'In-Ca-Sig': cachedFingerprint,
      },
    },
  };
};
```

## 关键设计说明

### 为什么用自定义 Header 而不是 Cookie？

| | Cookie | 自定义 Header |
|---|---|---|
| **自动携带** | 浏览器自动附带 | 需代码手动设置 |
| **XSS 窃取** | HttpOnly 可防 | JS 可读取但攻击者不知道计算逻辑 |
| **跨域** | 受 SameSite 影响 | 需 CORS 允许 |
| **设备绑定** | 无法绑定设备 | 值来自设备特征计算 |

自定义 Header 的核心优势：**值是从当前设备硬件特征实时计算的，不是一个静态的可迁移令牌**。

### Header 名称为什么用 In-Ca-Sig？

使用非语义化名称，不暴露用途。`In-` 为 Ingot 平台自定义 Header 前缀；`Ca` 可理解为 Client Authentication，`Sig` 可理解为 Signature。避免使用 `X-Device-Fingerprint` 这类直白的名称。

### 指纹的稳定性

| 维度 | 变化频率 | 影响 |
|------|----------|------|
| User-Agent | 浏览器大版本升级 | 低频，可接受 |
| 屏幕分辨率/色深 | 几乎不变 | 稳定 |
| Canvas | GPU 驱动更新 | 低频 |
| WebGL (GPU 信息) | 换显卡 | 极低频 |
| Audio | 音频驱动更新 | 低频 |
| 时区/语言 | 几乎不变 | 稳定 |

**综合稳定性：同一设备+浏览器，指纹在数周到数月内保持一致。** 当浏览器大版本升级导致指纹变化时，用户重新登录即可（session 会存储新指纹）。

### 指纹碰撞率

理论上具有相同硬件配置+相同浏览器版本+相同系统设置的不同设备可能产生相同指纹。但这不是问题，因为：

1. 指纹的目的是**绑定 Cookie 到设备**，不是唯一标识用户
2. 攻击者需要同时拥有 Cookie 值 + 相同的设备指纹，概率极低
3. 即使碰撞，攻击者还需要通过 Origin/Referer 白名单校验

### 降级兼容

当前端未携带 `In-Ca-Sig` Header 时，BFF 和网关会自动降级为 IP+UA 方式计算指纹，确保：

- 旧版前端在改造前仍可使用
- 非浏览器客户端（如移动 App）可以使用自己的设备标识
- 开发调试时不强制依赖前端改造

## CORS 配置注意

网关需要允许 `In-Ca-Sig` Header 通过 CORS：

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-headers:
              - "*"
            # 或明确列出: 
            # - In-Ca-Sig
            # - Content-Type
            # - Authorization
```
