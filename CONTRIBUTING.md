# 貢獻指南

感謝你對「網站健康檢查」有興趣。歡迎提出 Issue、改善文件或提交 Pull Request。

## 1. 開發環境

- JDK 17 以上
- Android Studio（建議最新穩定版）
- Android SDK Platform 36
- Android Build Tools 36.0.0

```bash
git clone https://github.com/mark216tw/website-health-check.git
cd website-health-check
./gradlew testDebugUnitTest
```

Windows：

```powershell
.\gradlew.bat testDebugUnitTest
```

## 2. 分支與提交

1. 從 `main` 建立功能分支：

```bash
git checkout -b feature/your-feature
```

2. 完成修改後，確認：

```bash
./gradlew lintDebug test assembleDebug
```

3. 提交訊息建議使用 Conventional Commits：

```text
feat: 新增檢查結果詳情
fix: 修正跨午夜排程計算
docs: 更新使用指南
refactor: 抽出 CSV 工具
```

4. 建立 Pull Request，說明：
   - 變更目的
   - 主要修改
   - 測試方式
   - 已知限制

## 3. 程式風格

- 使用 Kotlin 官方風格（4 空格縮排）。
- Compose 畫面保持可組合函式精簡，狀態交由 ViewModel。
- 網路、資料庫與檔案 IO 不得在主執行緒執行。
- 新增錯誤類型時，同步更新 `network/ErrorType.kt` 與測試。
- 新增使用者可見文字請使用繁體中文。
- 不要提交 `local.properties`、keystore、密碼或建置產物。

## 4. 測試要求

- 排程邏輯變更：更新 `ScheduleCalculatorTest`。
- 網路檢查變更：更新 `HealthCheckerTest`。
- CSV 或排序變更：更新對應測試。
- UI 變更：至少提供手動測試步驟與截圖（若有）。

## 5. 文件

文件位於根目錄與 `docs/`：

- `README.md`
- `CHANGELOG.md`
- `docs/USER_GUIDE.md`
- `docs/ARCHITECTURE.md`
- `docs/DESIGN.md`
- `docs/BUILD_AND_RELEASE.md`
- `docs/TESTING.md`
- `docs/SECURITY.md`

功能或行為變更時請同步更新相關文件。

## 6. 授權

提交貢獻即表示你同意以本專案的 [MIT License](LICENSE) 授權你的貢獻。
