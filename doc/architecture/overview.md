# アーキテクチャ概要（Tosk Backend）


> 本文書は Tosk Backend（Spring Boot）のアーキテクチャ全体像を示す。ER ドメイン（User/Team/UserTeam/Task/Comment/Like/Notification）を基盤とし、依存関係・レイヤ構造・データフロー・将来拡張性を明文化する。

---

## 1. 全体構成

```
ユーザー
  ↓ (HTTP/JSON)
API (Controller 層)
  ↓
アプリケーション層 (Service, Policy)
  ↓
ドメイン層 (Entity, Value, DomainService)
  ↓
インフラ層 (JPA Repository, 外部API Client)
  ↓
DB (PostgreSQL) / 外部サービス
```

* **API 層**: OpenAPI 仕様に基づくインターフェースを提供。DTO のシリアライズ/デシリアライズ、バリデーションを担当。
* **アプリケーション層**: ユースケース（例: タスク作成、コメント投稿）。複数ドメインを調整し、権限ポリシーを適用。
* **ドメイン層**: ビジネスルールの中核。エンティティ（User, Team, Task など）、値オブジェクト、ドメインサービスを保持。
* **インフラ層**: DB・外部サービスアクセスの実装。ドメイン層のリポジトリインターフェースを具象化。

---

## 2. ドメインモデル概要

* **User**: 認証主体。プロフィール情報、作成タスク/コメント/いいね/通知を保持。
* **Team**: 複数ユーザーを束ねる。オーナーを持つ。
* **UserTeam**: ユーザーとチームの関係（role: leader/member, status: pending/joined/rejected）。
* **Task**: タスクの中心。属性: title, description, due\_date, priority, visibility。like\_count/comment\_count を保持。
* **Comment**: タスクへのコメント。スレッド型（parent\_comment\_id を持つ）。
* **Like**: Task または Comment に対するいいね。
* **Notification**: イベントをユーザーに通知（join\_request\_approved など）。

---

## 3. レイヤ依存関係

* **API 層 → アプリケーション層**

    * DTO を受け取り、ユースケースサービスを呼び出す。
* **アプリケーション層 → ドメイン層**

    * ビジネスルールを適用。可視性・権限制御をポリシーとして切り出す。
* **ドメイン層 → インフラ層**

    * 永続化・外部通信はリポジトリ/ゲートウェイに委譲。
* **逆依存はなし**: ドメイン層はインフラ層を知らない。

---

## 4. データフロー例

### タスク作成 (Task.create)

1. API 層: `POST /api/v1/tasks` リクエストを受け、DTO をバリデーション。
2. アプリケーション層: CreateTaskService が呼ばれる。
3. ポリシー: TaskVisibilityPolicy が team/public 作成権限を確認。
4. ドメイン層: Task エンティティ生成。
5. インフラ層: TaskRepository.save() により DB に保存。
6. 通知生成が必要なら NotificationRepository.save() を呼ぶ。

### コメント投稿 (Comment.add)

1. API 層: `POST /api/v1/tasks/{id}/comments`
2. アプリケーション層: CreateCommentService が呼ばれる。
3. ドメイン層: Comment エンティティ生成。Task.comment\_count を更新。
4. インフラ層: CommentRepository.save()。
5. 通知を生成し、NotificationRepository.save()。

---

## 5. 非機能的アーキ要素

* **セキュリティ**: Spring Security + JWT (HttpOnly/Secure Cookie)。可視性 (private/team/public) と UserTeam.status に基づく認可。
* **DB運用**: PostgreSQL + Flyway。ID は UUID。監査列 (created\_at/updated\_at/deleted\_at)。
* **観測性**: Spring Boot Actuator, 構造化ログ(JSON), リクエストID。
* **品質ゲート**: Spotless, Checkstyle, PMD, SpotBugs, JaCoCo。

---

## 6. 将来拡張

* **モジュール分割**: `tosk-domain`, `tosk-app`, `tosk-api`, `tosk-infra`。
* **通知のリアルタイム化**: SSE / WebSocket。
* **外部連携**: GitHub Issues, Slack, カレンダー。
* **検索強化**: 全文検索・保存フィルタ。

---
