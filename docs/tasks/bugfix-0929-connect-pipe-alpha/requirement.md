# Requirement

## Goal

修正 `fragment_gutter_basic_info.xml` 中「無法開蓋」或「銜接點」勾選後，連接管欄位與其他被停用欄位的半透明效果不一致問題。

## Functional Requirements

- `layoutConnectPipe` 不得因父層與 `rgConnectPipe` 同時套用 alpha 而產生額外變暗。
- `rgConnectPipe` 的停用視覺效果應與材質、溝體結構受損、附掛或過路管線、淤積程度一致。
- 連接管標題列與必填提示在上述狀態下應維持完整透明度。
- 不改變「無法開蓋」與「銜接點」原有的互斥、禁用及資料清除行為。

## Scope Exclusion

`pbPhotoUploadStatus1` 動畫問題已確認為測試機動畫設定，不納入本次程式修正。
