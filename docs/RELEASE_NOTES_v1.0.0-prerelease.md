# 網站健康檢查 v1.0.0-prerelease

> **此版本為 Pre-release 測試版本，不是正式上線版本。**
>
> APK 使用 Android Debug 金鑰簽署，僅供測試與回饋使用。

## 版本資訊

- 版本名稱：`1.0.0-prerelease`
- 版本代碼：`1`
- 建置類型：`prerelease`
- R8 壓縮：啟用
- 資源縮減：啟用
- 簽署：Android Debug 金鑰
- 最低 Android 版本：8.0（API 26）
- 目標 Android 版本：API 36

## 下載

- Asset：`app-prerelease.apk`

## 本次重點

- 多網站監控與啟用／停用
- 排程：星期、每日時段、間隔（最低 15 分鐘）
- 首次／儲存後立即檢查，之後依上次檢查時間加上間隔
- 時間／時區變更自動重排與排程診斷
- HTTP 狀態、轉址、逾時、TLS、內容字串檢查
- 異常／恢復通知與失敗門檻
- 檢查紀錄、詳情、複製與 CSV 匯出（全部／篩選）
- 網站排序、顯示模式與系統列主題同步
- 重複網址提示、HTTP 明文警告、錯誤類型中文化
- 過期紀錄自動清理與電池最佳化導引

## 安裝方式

1. 下載 `app-prerelease.apk`。
2. 允許安裝未知來源應用程式。
3. 安裝後開啟 App，並依需求允許通知權限。
4. 新增網站後，若在檢查時段內會立即檢查一次。

## 驗證資訊

- `versionName`：`1.0.0-prerelease`
- 簽章：`CN=Android Debug`
- SHA-256：

```text
FC3A5E60FB746C459CF24CA6D11C10CDB4B1E0599BE12A32D6B309553E1D4FC2
```

## 已知限制

- WorkManager 可能受 Android Doze／省電模式延遲，無法保證分秒準時。
- 部分 OEM 對背景工作限制較嚴格，建議將 App 設為不受電池最佳化限制。
- 內容字串檢查只比對前 1 MB 原始回應，且區分大小寫；不執行 JavaScript。
- 目前僅提供繁體中文介面。
- 此版本使用 Debug 金鑰簽署，請勿用於正式環境。

## 回饋

若發現問題，請於 GitHub Issues 回報，並附上：

- 裝置型號與 Android 版本
- 網站設定（請遮蔽敏感網址）
- 檢查紀錄中的錯誤類型與訊息
- 重現步驟

## 授權

MIT License
