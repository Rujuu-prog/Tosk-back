# セキュリティポリシー（Tosk Backend）

> 本文書は Tosk Backend（Spring Boot）のセキュリティポリシーを定義する。ER ドメイン（User/Team/UserTeam/Task/Comment/Like/Notification）を前提とし、認証・認可・入力検証・監査・運用の観点を明文化する。

---

## 1. 認証

* **方式**: Spring Security + JWT。
* **保存先**: HttpOnly + Secure + SameSite=Strict クッキー。
* **期限**: アクセストークンは短命（15〜30分）、リフレッシュトークンは長命（数日〜数週間）。
* **更新**: リフレッシュフローで安全に再発行。
* **ログイン試行制限**: 連続失敗時は一時ロックアウト。

---

## 2. 認可

* **Task.visibility** に基づく制御：

    * private: 作成者のみ閲覧/操作可。
    * team: UserTeam.status = joined のメンバーが閲覧可。編集/削除は作成者 or leader のみ。
    * public: ログイン済み全ユーザー閲覧可。編集/削除は作成者 or leader のみ。
* **UserTeam.role**：

    * leader: メンバー承認/拒否、ロール変更、強制退出が可能。
    * member: 通常操作のみ。
* **Like/Comment/Notification**: 閲覧可能性は紐づく Task の visibility に従う。

---

## 3. 入力検証

* **DTO レベル**: Bean Validation で必須・文字数・列挙値を検証。
* **ドメインレベル**: ビジネスルールを検証（例: Like の重複禁止）。
* **サイズ制限**:

    * Task.title: 1〜120文字
    * Task.description: ≤10,000文字
    * Comment.content: ≤5,000文字
* **ファイル/画像**: 拡張子と MIME タイプを検証。サイズ上限 5MB。

---

## 4. データ保護

* **パスワード**: bcrypt/argon2 で強ハッシュ。
* **機微情報**: 平文保存禁止。必要に応じ暗号化カラムを利用。
* **通信**: TLS 1.2+ を必須。HTTP → HTTPS リダイレクト。

---

## 5. 監査・ロギング

* **監査対象**:

    * UserTeam.status 遷移（pending→joined/rejected）
    * Task.visibility 変更
    * 削除操作（ソフトデリート含む）
* **ログ形式**: 構造化 JSON。`requestId` と `userId` を必須項目に。
* **保存期間**: 本番では最低90日。

---

## 6. 脅威モデルと対策

* **スパム/DoS**: レート制限（IP単位/ユーザー単位）、CAPTCHA 導入検討。
* **権限昇格**: サーバサイドでの必須チェック。フロントの UI 制御に依存しない。
* **ID 推測**: UUID v4 を利用し、連番 ID を避ける。
* **CSRF**: stateless API で無効化。ただし Origin/Referer チェックを併用。
* **XSS**: 出力エスケープを徹底。入力検証で HTML/script を制限。
* **SQL Injection**: JPA + パラメータバインドを徹底。

---

## 7. 運用ポリシー

* **脆弱性管理**: Dependabot などで依存ライブラリを監視。
* **セキュリティテスト**: CI/CD で静的解析・依存スキャン。定期的にペンテスト実施。
* **秘密情報管理**: 環境変数 or Secrets Manager を利用。リポジトリに直書き禁止。

---

## 8. 今後の強化ポイント

* Web Push / Email 通知におけるセキュリティ設計。
* 2FA（多要素認証）の導入検討。
* RBAC を越える ABAC（属性ベース認可）の導入可能性。
* セッションハイジャック検知（異常 IP/UA の検出）。

---

> 設計・レビュー時に常に参照する。
