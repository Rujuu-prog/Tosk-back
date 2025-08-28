# Database Design (Tosk Backend)

この文書は本バックエンドのDB設計方針（制約・削除ポリシー・集計・運用）をまとめたものです。

## 方針サマリ
- ID/監査: すべての主要テーブルは `UUID` 主キー、`created_at`/`updated_at`/`deleted_at` を持つ。
- 一意性: ソフトデリート（`deleted_at is null`）な行のみ一意を担保する「部分一意インデックス」を採用。
- 削除ポリシー:
  - コメント: 論理削除（`deleted_at` を設定）。復元可能・監査性を優先。
  - いいね: 物理削除。軽量メタデータのためシンプルさを優先。
- 集計値: `tasks.comment_count` と `tasks.like_count` をDBトリガで自動整合。論理削除にも追随。
- 可視性: `tasks.visibility` が `team` の場合は `team_id` 必須（チェック制約）。

## 主なテーブル
- `users(email, username)` は大小無視の部分一意インデックス。
- `teams(name)` は大小無視の部分一意インデックス（`deleted_at is null`）。
- `user_team` は `(user_id, team_id)` の重複参加を禁止。
- `tasks` は `author_id`, `team_id`（null可）, `visibility`, `priority`, `due_date` など。
- `comments` は `task_id`, `author_id`, `parent_comment_id`（スレッド）を保持。論理削除。
- `task_like`/`comment_like` は物理削除。重複いいねを禁止する一意制約。
- `notifications` は JSONB `payload` を持つ。未読に部分インデックス。

## インデックス設計（抜粋）
- 文字列の一意性: `lower(column)` の式インデックス（国際化要件が強い場合は `citext` に移行検討）。
- 一覧・検索向け: `tasks(due_date) where deleted_at is null`, `tasks(priority) ...`。
- コメント取得: `comments(task_id) where deleted_at is null`。

## 削除ポリシー詳細
- コメント（論理削除）
  - `deleted_at` の null⇔非null 更新時に `tasks.comment_count` を加減算するトリガを実装。
  - 物理削除時も、削除対象が「有効」だった場合のみ減算。
  - 復元（undelete）で増算されるため、モデレーションのワークフローを支援。
- いいね（物理削除）
  - insert/delete のみに追随。論理削除列は持たない。

## 運用メモ
- Seed: `docker` プロファイル時のみ `db/dev/R__seed.sql` を適用（本番は適用されない）。
- パージ: 論理削除データの保持期間が定義される場合、定期パージジョブを別途実装。
- 監査拡張: 必要に応じ `deleted_by`/`deleted_reason`/`moderation_flag` を追加可能。

## 将来拡張
- `citext` 拡張採用による大小無視の単純化（email/username/team name）。
- 生成列 + インデックスで `notifications.payload` の頻出キーを最適化。
- RLS（Row-Level Security）やテナンシー分離の導入検討。

