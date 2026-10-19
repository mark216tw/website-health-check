# 測試文件

## 1. 測試策略

目前以 JVM 單元測試為主，涵蓋：

- 排程時間計算
- HTTP 健康檢查結果
- 錯誤類型顯示
- CSV 產生與跳脫
- 網站排序
- 設定持久化

UI、WorkManager 實際排程、Room migration、通知與 R8 執行期行為需透過實機或模擬器驗證。

## 2. 單元測試

執行：

```bash
./gradlew testDebugUnitTest
```

測試檔位置：`app/src/test/java/com/websitecheck/app/`

| 測試類別 | 數量 | 內容 |
|---|---:|---|
| `ScheduleCalculatorTest` | 13 | 首次立即檢查、時段邊界、跨午夜、相對間隔、逾期補檢查、間隔下限、停用日 |
| `HealthCheckerTest` | 10 | 成功、狀態碼失敗、轉址、停用轉址、無效網址、內容字串、大小寫、強制 GET、URL 正規化 |
| `ErrorTypeTest` | 3 | 錯誤類型中文標籤與未知類型 |
| `LogCsvTest` | 2 | CSV header、BOM、逗號／引號／換行跳脫 |
| `SiteSortOptionTest` | 6 | 名稱、建立時間、狀態、回應時間、null 排序 |
| `SettingsRepositoryTest` | 3 | 設定持久化與預設值 |

目前合計：**37 項測試**。

## 3. Lint 與建置

```bash
./gradlew lintDebug
./gradlew assembleDebug
./gradlew assemblePrerelease
```

`lintDebug` 應為 0 error。既有 warning 主要為相依套件版本更新提示與資源資料夾提示。

## 4. 手動測試清單

### 4.1 網站管理

- [ ] 新增網站後可立即看到檢查紀錄
- [ ] 編輯網站後設定保留
- [ ] 刪除網站後紀錄一併消失
- [ ] 停用網站後不再排程
- [ ] 重新啟用後立即檢查
- [ ] 重複網址顯示確認視窗
- [ ] 輸入 `http://` 顯示明文警告

### 4.2 檢查流程

- [ ] HEAD 檢查成功
- [ ] HEAD 被拒時 fallback GET
- [ ] 狀態碼超出範圍顯示異常
- [ ] 轉址開啟／關閉行為正確
- [ ] 內容字串存在時成功
- [ ] 內容字串不存在時顯示「內容未包含指定字串」
- [ ] 大小寫不同視為不匹配
- [ ] 逾時顯示「連線逾時」
- [ ] 無效網域顯示「DNS 解析失敗」

### 4.3 排程

- [ ] 儲存後立即檢查
- [ ] 下次時間為上次檢查 + 間隔
- [ ] 時間區段外不檢查
- [ ] 跨午夜時段正確
- [ ] 跨日／跨星期正確
- [ ] 變更時間或時區後排程重新校正
- [ ] 設定頁「排程診斷」狀態正確
- [ ] 按「重新排程」後狀態更新

### 4.4 通知

- [ ] 達失敗門檻發送異常通知
- [ ] 恢復後發送恢復通知
- [ ] 同一次連續失敗不重複通知
- [ ] 系統通知關閉時顯示提示
- [ ] 點擊通知可開啟 App

### 4.5 紀錄與匯出

- [ ] 全部／成功／失敗篩選
- [ ] 網站篩選
- [ ] 點擊紀錄顯示詳情
- [ ] 複製詳情
- [ ] 匯出目前篩選 CSV
- [ ] 設定頁匯出全部 CSV
- [ ] Excel 開啟 CSV 中文正常
- [ ] 保留天數變更後舊紀錄清除

### 4.6 顯示與排序

- [ ] 系統／淺色／深色切換立即生效
- [ ] 狀態列與導覽列圖示對比正確
- [ ] 9 種排序皆正確
- [ ] 排序設定重啟後保留

### 4.7 Pre-release R8

- [ ] 安裝 `app-prerelease.apk`
- [ ] App 可啟動
- [ ] 可新增網站並執行檢查
- [ ] WorkManager 可執行背景檢查
- [ ] TimeChangeReceiver 可收到時間變更
- [ ] 通知可發送
- [ ] CSV 可匯出

## 5. 已知測試缺口

- 無 Compose UI instrumentation 測試。
- 無 WorkManager instrumentation 測試。
- 無 Room migration instrumentation 測試。
- 無多裝置／多 OEM 測試。
- R8 版本僅完成建置與類別保留檢查，尚未在實機完整回歸。
- Doze、省電模式與 OEM 背景限制需人工驗證。

## 6. 測試資料建議

- 正常網站：`https://example.com`
- 內容字串：`Example Domain`
- 錯誤狀態：可測試回傳 404／500 的端點
- 轉址：可測試 301／302 的端點
- 逾時：可使用不可達或延遲回應的測試伺服器

請勿對未經授權的網站進行高頻檢查。
