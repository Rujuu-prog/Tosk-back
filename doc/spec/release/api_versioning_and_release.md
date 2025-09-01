# API/クライアントのバージョニングとリリース運用

本書は OpenAPI（仕様/クライアント）と Spring Boot（バックエンド）のバージョニング/リリース運用を規定する。目的は、後方互換性の担保、配布の一貫性、CIの自動化、消費者（Next.js）の安全な更新である。

---

## 1. バージョニング原則（SemVer）

- 方式: Semantic Versioning（MAJOR.MINOR.PATCH）。
- 対象:
  - バックエンド（アプリ）: Gradle の `version` を採用（例: `1.4.2`）。
  - OpenAPI 仕様: `src/main/resources/openapi/api_internal.yaml` の `info.version` を採用。
  - フロント用TSクライアント（npm）: パッケージの `version`。原則 `info.version` と同期。
- 互換性の定義（API視点）:
  - MAJOR: 破壊的変更（必須項目の追加/削除、型変更、意味的非互換、レスポンス必須化 など）。
  - MINOR: 非破壊的拡張（エンドポイント追加、任意プロパティ追加、nullable化、enum拡張）。
  - PATCH: 仕様文言/例/メタデータ修正、バグ修正（機能挙動不変）。
- APIパスのバージョン: `/v1` を維持。破壊的変更は `/v2` を新設（移行期間を設け、`/v1` と併存）。

---

## 2. Gitタグ戦略（同一リポでの共存）

タグのプレフィックスで責務を分離し、同一リポ内で衝突なく管理する。

- バックエンド（Spring Boot）: `backend-v<MAJOR.MINOR.PATCH>`
  - 例: `backend-v1.8.0`
  - CI: Dockerイメージ/リリース資産の公開、サーバの配布。
- OpenAPIクライアント（npm）: `openapi-client-v<MAJOR.MINOR.PATCH>`
  - 例: `openapi-client-v1.4.0`
  - CI: TSクライアント生成 → GitHub Packages（private）へ publish。
- 任意（仕様タグ）: `openapi-spec-v<MAJOR.MINOR.PATCH>`（必要時）。

> どのタグも同じリポジトリで共存可能。CI はタグのプレフィックスで分岐させる。

---

## 3. CI ゲートと公開フロー

- 推奨トリガー: タグ駆動（`openapi-client-v*` / `backend-v*`）。
- OpenAPI差分検査（任意だが推奨）:
  - `openapi-diff` 等で前回公開版のspecと比較し、破壊的変更の有無を検出。
  - 破壊的変更なのに MAJOR を上げていない → CI失敗。
  - 非破壊なのに MAJOR を上げている → 警告（任意）。
- 同期チェック:
  - クライアント公開時は `info.version` と npm の `version` 一致を検証。
  - バックエンドの `version` と `info.version` は一致「必須」ではない（アプリ内部の非機能変更を許容）。ただしメジャーは概ね整合することが望ましい。

---

## 4. TSクライアント配布（GitHub Packages, private）

- パッケージ名: `@<OWNER>/tosk-openapi-client`（スコープ=GitHub org/user）。
- チャネル:
  - 安定: `latest`（通常の `npm i`）。
  - 先行: `next`（`npm i @<OWNER>/tosk-openapi-client@next`）。`-beta.N`/`-rc.N` を `--tag next` で公開。
- Next.js（消費者）側の設定例:
  - `.npmrc`
    - `@<OWNER>:registry=https://npm.pkg.github.com`
    - `//npm.pkg.github.com/:_authToken=${GITHUB_TOKEN}`（読み取り権限）
  - 依存: `yarn add @<OWNER>/tosk-openapi-client`
  - Cookie送信: 生成クライアントのAxiosへ `withCredentials: true` を設定。

---

## 5. デプリケーション運用

- 非推奨: OpenAPIに `deprecated: true` と説明（移行先を明記）。
- 撤去: 非推奨から撤去まで最低1リリースサイクル（例: 90日）。
- CHANGELOG: 非推奨開始時と撤去時に記載（対象エンドポイント/フィールド）。

---

## 6. CHANGELOGとコミット規約

- Conventional Commits を推奨。
  - `feat:` → MINOR、`fix:`/`chore:`/`docs:` → PATCH。
  - 破壊的変更: `BREAKING CHANGE:` フッター必須 → MAJOR。
- CHANGELOG は自動生成（release-please/changesets等）。

---

## 7. 実務手順（例）

1) 仕様変更 → `api_internal.yaml` を更新（`info.version` も適切に更新）。
2) ローカルで生成/検証
   - `./gradlew openApiGenerateBackend`（Java インタフェース/モデル）
   - `./gradlew openApiGenerateFrontend`（開発用にTS生成）
3) PR 作成 → レビュー/マージ。
4) リリース
   - TSクライアント: `openapi-client-v1.4.0` をタグプッシュ → CIでpublish。
   - バックエンド: `backend-v1.8.0` をタグプッシュ → CIでDocker/リリース。

---

## 8. よくある質問

- Q: 同一リポで OpenAPI と Spring Boot のリリースは管理し切れる？
  - A: 可能。タグのプレフィックス（`openapi-client-v*` と `backend-v*`）で責務を分離し、CIを分岐すれば衝突しない。`info.version` とアプリ `version` は厳密一致不要だが、メジャー整合は推奨。
- Q: 仕様だけ更新したい場合は？
  - A: `openapi-client-v*` タグのみ打つ。サーバ無更新でクライアントだけ更新できる（非破壊の前提で）。
- Q: 破壊的変更を入れたい場合は？
  - A: `/v2` を作り、`info.version` の MAJOR を上げ、クライアントも MAJOR を上げる。移行期間は `/v1` と `/v2` を併存。

---

## 9. 参考

- OpenAPI Generator（server: `spring`, client: `typescript-axios`）
- GitHub Packages (npm) private publish
- openapi-diff（Breaking/Non-breaking の自動判定に利用）

