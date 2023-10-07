---
layout: default
title: FAQ
nav_order: 200
permalink: /faq
---

## 413 Request Entity Too Large

### 问题

```bash
[SCANNER] o.a.s.ctl.client.ArchGuardHttpClient process topic: class-items
[SCANNER] o.a.s.ctl.client.ArchGuardHttpClient                 response status: 413
response body: <html>
<head><title>413 Request Entity Too Large</title></head>
<body>
<center><h1>413 Request Entity Too Large</h1></center>
<hr><center>nginx/1.22.1</center>
</body>
</html>
``` 

### 解决方案

需要配置 Nginx 的 `client_max_body_size` 参数，否则会出现 413 的错误。

示例：

```nginx
http {
    ...
    client_max_body_size 20m;
    ...
}
```

## Server 模块包体积过大

模块比较大的模块：

- SentenceTransformers，需要使用 OnnxRuntime 和 Tokenizer 模块，体积：80M+
- Code Interpreters，需要使用 Kotlin 编译器，体积： ~70M+
