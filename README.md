# ViaX

## 构建

用 Android Studio 打开本目录，等待 Gradle 同步，然后运行 `app`。已配置 Android Gradle Plugin 9.4.0、Gradle 9.6.0、JDK 17+，需要 Android SDK API 35。最低 Android 版本为 9.0（API 28），以确保独立进程的无痕 WebView 数据目录可用。

命令行构建（Windows）：

```powershell
.\gradlew.bat :app:assembleDebug
```
