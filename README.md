# 網站健康檢查（Website Health Check）

以 Kotlin 與 Jetpack Compose 打造的 Android 網站監控 App。可設定多個網址，依指定的星期、每日時間區段與檢查頻率自動檢查，並保留檢查紀錄、發送異常／恢復通知，且支援內容字串比對與 CSV 匯出。

> **目前版本：`1.0.0-prerelease`**
>
> 這是 Pre-release 測試版本，使用 Debug 金鑰簽署，**不是正式上線版本**。

## 功能特色

- 多網站管理：新增、編輯、刪除、啟用／停用
- 多種檢查設定：
  - HTTP `HEAD`／`GET`
  - 自訂逾時、預期 HTTP 狀態碼範圍
  - 是否跟隨轉址
  - 可選內容字串檢查（GET、比對原始回應前 1 MB、區分大小寫）
- 自動排程：
  - 選擇檢查星期
  - 設定每日檢查時間區段（支援跨午夜）
  - 設定檢查頻率（最低 15 分鐘）
  - 儲存或啟用後，若在時段內會立即檢查一次
  - 下次檢查以「上次檢查時間 + 間隔」計算
- 檢查紀錄：
  - 成功／失敗、HTTP 狀態、回應時間、錯誤類型
  - 依網站與結果篩選
  - 點擊查看完整詳情、最終轉址網址，並可複製
  - 匯出全部或目前篩選的 CSV
- 通知：
  - 連續失敗達門檻時發送異常通知
  - 恢復正常時發送恢復通知
  - 顯示系統通知權限狀態
- 網站排序：9 種排序方式，設定會保留
- 顯示模式：系統／淺色／深色，系統列與導覽列同步
- 排程診斷：查看 WorkManager 狀態、下次／上次檢查時間、逾期標示與看門狗狀態，並可重新整理或重新排程
- 逾期自動修復：看門狗每 15 分鐘校正，開啟 App 也會立即補檢查
- 電池最佳化導引與過期紀錄自動清理

## 下載

- 最新 Pre-release：<https://github.com/mark216tw/website-health-check/releases/tag/v1.0.0-prerelease>
- 所有版本：<https://github.com/mark216tw/website-health-check/releases>

安裝檔名：`app-prerelease.apk`

> 此 APK 使用 Android Debug 金鑰簽署，僅供測試。請勿作為正式發行版本散布。

## 系統需求

- Android 8.0（API 26）以上
- 需要網路連線
- Android 13（API 33）以上需允許通知權限

## 快速開始

1. 安裝 `app-prerelease.apk`。
2. 開啟 App，於提示時允許通知。
3. 點擊右下角「＋」新增網站。
4. 輸入網站名稱與網址，設定檢查排程與需要的檢查條件。
5. 儲存後，若目前時間在檢查時段內，會立即檢查一次。
6. 在「檢查紀錄」查看結果；在「設定」調整通知、保留天數與顯示模式。

完整操作說明請見 [使用指南](docs/USER_GUIDE.md)。

## 建置

需求：

- JDK 17 以上（建議 17 或 21）
- Android SDK Platform 36
- Android Build Tools 36.0.0

```bash
# Debug
./gradlew assembleDebug

# Pre-release（R8 + Debug 簽署）
./gradlew assemblePrerelease
```

Pre-release APK 輸出：

```text
app/build/outputs/apk/prerelease/app-prerelease.apk
```

版本名稱：`1.0.0-prerelease`  
簽署：Android Debug 金鑰  
壓縮：R8（`isMinifyEnabled = true`）與資源縮減

更多建置與發行說明請見 [建置與發行](docs/BUILD_AND_RELEASE.md)。

## 專案結構

```text
.
├── app/                         Android 應用程式模組
│   ├── src/main/java/...        Kotlin 原始碼
│   ├── src/main/res/            資源與主題
│   └── src/test/java/...        單元測試
├── docs/                        專案文件
├── gradle/                      Gradle Wrapper 與版本目錄
├── build.gradle.kts
├── settings.gradle.kts
├── LICENSE                      MIT License
└── README.md
```

## 技術棧

| 類別 | 技術 |
|---|---|
| 語言 | Kotlin 2.1.20 |
| UI | Jetpack Compose、Material 3 |
| 導覽 | Navigation Compose |
| 資料庫 | Room 2.6.1 |
| 背景排程 | WorkManager 2.10.1 |
| 網路 | OkHttp 4.12.0 |
| 非同步 | Kotlin Coroutines / Flow |
| 建置 | Gradle 8.11.1、AGP 8.10.1 |

## 文件

| 文件 | 說明 |
|---|---|
| [使用指南](docs/USER_GUIDE.md) | 安裝、設定與操作說明 |
| [系統架構](docs/ARCHITECTURE.md) | 分層架構、資料流、排程與網路檢查 |
| [系統設計](docs/DESIGN.md) | 功能需求、資料模型、排程規則與設計決策 |
| [建置與發行](docs/BUILD_AND_RELEASE.md) | Debug／Pre-release 建置、R8、簽署與 Release |
| [測試文件](docs/TESTING.md) | 單元測試、測試指令與手動測試清單 |
| [安全性](docs/SECURITY.md) | 權限、資料儲存、HTTP／TLS 與漏洞回報 |
| [貢獻指南](CONTRIBUTING.md) | 開發流程與 PR 規範 |
| [變更紀錄](CHANGELOG.md) | 版本變更摘要 |

## 已知限制

- WorkManager 受 Android Doze／省電機制影響，背景檢查可能延遲。
- 部分 OEM（小米、華為、三星等）對背景工作限制較嚴格，建議在系統設定中將 App 設為不受電池最佳化限制。
- 內容字串檢查只比對前 1 MB 的原始回應內容，且區分大小寫。
- 目前僅提供繁體中文介面。
- Pre-release 版本使用 Debug 金鑰簽署，不適合正式上架。
- 尚未提供 TLS 憑證到期檢查、Widget、雲端同步或前景服務常駐檢查。

## 授權

本專案採用 [MIT License](LICENSE)。

Copyright (c) 2026 mark216tw
