# 建置與發行

## 1. 環境需求

| 項目 | 版本 |
|---|---|
| JDK | 17 以上（本專案以 JDK 17／21 驗證） |
| Android SDK Platform | 36 |
| Android Build Tools | 36.0.0 |
| Gradle | 8.11.1（Wrapper） |
| Android Gradle Plugin | 8.10.1 |

請確認 `local.properties` 或 `ANDROID_HOME` 指向正確的 Android SDK。

```properties
sdk.dir=C\:\\Users\\<使用者>\\AppData\\Local\\Android\\Sdk
```

> `local.properties` 已被 `.gitignore` 排除，不應提交。

## 2. 常用建置指令

### Debug

```bash
./gradlew assembleDebug
```

輸出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 單元測試

```bash
./gradlew testDebugUnitTest
```

### Lint

```bash
./gradlew lintDebug
```

### Release

```bash
./gradlew assembleRelease
```

目前 `release` build type：

- 未啟用 R8（`isMinifyEnabled = false`）
- 未設定正式簽署
- 版本名稱 `1.0.0`

若要正式上架，請另外設定 release signingConfig 與 R8 規則。

## 3. Pre-release 建置

`prerelease` build type 設計為「可安裝的測試發行版」：

| 設定 | 值 |
|---|---|
| 初始化來源 | `release` |
| R8 壓縮 | 啟用 |
| 資源縮減 | 啟用 |
| 簽署 | Android Debug 金鑰 |
| 版本名稱 | `1.0.0-prerelease` |
| 版本代碼 | 1 |

設定位置：`app/build.gradle.kts`

```kotlin
create("prerelease") {
    initWith(getByName("release"))
    isMinifyEnabled = true
    isShrinkResources = true
    signingConfig = signingConfigs.getByName("debug")
    versionNameSuffix = "-prerelease"
    matchingFallbacks += listOf("release")
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
    )
}
```

建置：

```bash
./gradlew assemblePrerelease
```

輸出：

```text
app/build/outputs/apk/prerelease/app-prerelease.apk
```

### 3.1 R8 保留規則

`app/proguard-rules.pro`：

```proguard
-keep class com.websitecheck.app.work.CheckWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

-keep class com.websitecheck.app.work.TimeChangeReceiver { *; }

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
```

原因：

- WorkManager 以反射建立 `CheckWorker`。
- `TimeChangeReceiver` 由系統透過 Manifest 啟動。
- 保留 Room／Kotlin 需要的 annotation 與 metadata。

### 3.2 驗證 APK

```bash
# 版本資訊
$ANDROID_HOME/build-tools/36.0.0/aapt2 dump badging \
  app/build/outputs/apk/prerelease/app-prerelease.apk

# 簽章
$ANDROID_HOME/build-tools/36.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/prerelease/app-prerelease.apk

# SHA-256
sha256sum app/build/outputs/apk/prerelease/app-prerelease.apk
```

預期：

- `versionName='1.0.0-prerelease'`
- 簽章者為 `CN=Android Debug`
- APK 大小約 1.5 MB（實際依工具鏈版本略有差異）

> Debug 金鑰簽署僅適用測試。正式發行請使用專屬 release keystore，且不要提交金鑰或密碼。

## 4. GitHub Release 流程

本專案的 Pre-release：

- Tag：`v1.0.0-prerelease`
- Release 類型：Pre-release
- Asset：`app-prerelease.apk`

使用 GitHub CLI：

```bash
gh release create v1.0.0-prerelease \
  app/build/outputs/apk/prerelease/app-prerelease.apk \
  --title "網站健康檢查 v1.0.0-prerelease" \
  --notes-file docs/RELEASE_NOTES_v1.0.0-prerelease.md \
  --prerelease
```

若需更新既有 Release：

```bash
gh release upload v1.0.0-prerelease \
  app/build/outputs/apk/prerelease/app-prerelease.apk \
  --clobber
```

## 5. 版本命名

| 類型 | 格式 | 範例 |
|---|---|---|
| Debug | `versionName` | 1.0.0 |
| Pre-release | `versionName + "-prerelease"` | 1.0.0-prerelease |
| 正式 Release | 自行定義 | 1.0.0 |

`versionCode` 目前為 1。每次重新發佈應遞增 `versionCode`。

## 6. 發行前檢查清單

- [ ] `./gradlew lintDebug test assemblePrerelease`
- [ ] 確認 `versionName` 與 tag 一致
- [ ] 確認 APK 使用預期簽章
- [ ] 確認 R8 後 App 可啟動、可新增網站、可檢查、可收到通知
- [ ] 確認 `CheckWorker` 與 `TimeChangeReceiver` 未被移除
- [ ] 更新 `CHANGELOG.md` 與 Release Notes
- [ ] 上傳 APK 並標記 Pre-release
- [ ] 於 Release 說明標註「非正式上線版本」
