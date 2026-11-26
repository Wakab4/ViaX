# ViaX

## 功能增强

- 二维码扫描支持双指放大和缩小取景画面。
- 支持向网页中嵌套的 `iframe` 注入匹配的用户脚本。
- 移除资源嗅探和用户脚本注入的内置黑名单。
- 支持隐藏书签文件夹，可在书签页的「更多」中临时显示并恢复。
- 「设置 → 网站数据」可按 WebView 报告的存储大小排序，支持勾选删除指定网址的 Cookie，也可在列表中多选清理站点及其子域的 Cookie、缓存和网页存储。
- 在「设置 → 代理」为网页和内置下载配置 HTTP、HTTPS 或 SOCKS5 代理，用户名和密码可选。配置后可在浏览器菜单快速开关代理。

## 构建

安装 Android SDK 37，用 Android Studio 打开项目。Gradle 同步后运行 `app`。

在 Windows 上也可以用命令行构建。

```powershell
.\gradlew.bat :app:assembleDebug
```

## 免责声明

本仓库代码均由大语言模型（LLM）生成。
