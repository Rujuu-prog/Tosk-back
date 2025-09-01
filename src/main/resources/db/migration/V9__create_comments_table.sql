-- コメントテーブルの変更（V2で作成済みなので、必要な変更のみ実行）
-- author_id を user_id にリネーム
ALTER TABLE comments RENAME COLUMN author_id TO user_id;

-- like_count カラムを追加
ALTER TABLE comments ADD COLUMN like_count integer NOT NULL DEFAULT 0;

-- インデックスの作成 (V2で一部作成済みなので、IF NOT EXISTSを使用)
CREATE INDEX IF NOT EXISTS idx_comments_task_id ON comments(task_id);
CREATE INDEX IF NOT EXISTS idx_comments_user_id ON comments(user_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent_comment_id ON comments(parent_comment_id);
CREATE INDEX IF NOT EXISTS idx_comments_created_at ON comments(created_at);
CREATE INDEX IF NOT EXISTS idx_comments_deleted_at ON comments(deleted_at);

-- 削除されていないコメントのみを対象とする部分インデックス
CREATE INDEX IF NOT EXISTS idx_comments_task_id_not_deleted ON comments(task_id) WHERE deleted_at IS NULL;