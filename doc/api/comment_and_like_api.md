# コメント・いいね機能 API 仕様書（フロントエンド向け）

> 本文書は、Tosk Backendのコメント機能といいね機能のフロントエンド開発者向けAPI仕様書です。

---

## 概要

- **コメント機能**: タスクに対してスレッド型コメント（親子関係）を投稿・編集・削除
- **いいね機能**: タスクやコメントに対していいね/いいね取り消しをトグル操作
- **認証**: すべてのAPIでJWT認証（クッキー）が必要

---

## コメント機能 API

### 1. コメント作成

```
POST /api/tasks/{taskId}/comments
```

タスクに新しいコメントを追加します。

**パラメーター**
- `taskId` (path, required): タスクのUUID

**リクエストボディ**
```json
{
  "content": "コメント内容（1-5000文字）",
  "parentCommentId": "親コメントのUUID（返信の場合のみ）"
}
```

**レスポンス（201 Created）**
```json
{
  "id": "コメントID",
  "taskId": "タスクID",
  "userId": "作成者ID",
  "parentCommentId": "親コメントID（返信の場合）",
  "content": "コメント内容",
  "likeCount": 0,
  "createdAt": "2023-01-01T10:00:00Z",
  "updatedAt": "2023-01-01T10:00:00Z",
  "authorUsername": "作成者ユーザー名",
  "authorDisplayName": "作成者表示名",
  "authorAvatarUrl": "作成者アバターURL"
}
```

**エラーケース**
- `400 Bad Request`: バリデーションエラー、タスクが見つからない、親コメントが無効
- `401 Unauthorized`: 認証が必要

### 2. タスクのコメント一覧取得

```
GET /api/tasks/{taskId}/comments?page=0&size=20
```

指定タスクのコメント一覧を取得します（ページング対応）。

**パラメーター**
- `taskId` (path, required): タスクのUUID
- `page` (query, optional): ページ番号（デフォルト: 0）
- `size` (query, optional): 1ページあたりの件数（デフォルト: 20）

**レスポンス（200 OK）**
```json
{
  "content": [
    {
      "id": "コメントID",
      "taskId": "タスクID",
      "userId": "作成者ID",
      "parentCommentId": null,
      "content": "コメント内容",
      "likeCount": 5,
      "createdAt": "2023-01-01T10:00:00Z",
      "updatedAt": "2023-01-01T10:00:00Z",
      "authorUsername": "user1",
      "authorDisplayName": "ユーザー1",
      "authorAvatarUrl": null
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 15,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

### 3. コメント編集

```
PUT /api/comments/{commentId}
```

自分のコメントを編集します。

**パラメーター**
- `commentId` (path, required): コメントのUUID

**リクエストボディ**
```json
{
  "content": "編集後のコメント内容"
}
```

**レスポンス（200 OK）**
```json
{
  "id": "コメントID",
  "content": "編集後のコメント内容",
  "updatedAt": "2023-01-01T11:00:00Z"
}
```

**エラーケース**
- `400 Bad Request`: コメントが見つからない、他人のコメントを編集しようとした
- `401 Unauthorized`: 認証が必要

### 4. コメント削除

```
DELETE /api/comments/{commentId}
```

自分のコメントを削除します（ソフトデリート）。

**パラメーター**
- `commentId` (path, required): コメントのUUID

**レスポンス（204 No Content）**

**エラーケース**
- `400 Bad Request`: コメントが見つからない、他人のコメントを削除しようとした
- `401 Unauthorized`: 認証が必要

---

## いいね機能 API

### 1. タスクいいねトグル

```
POST /api/tasks/{taskId}/like
```

タスクのいいねを追加/削除します（トグル操作）。

**パラメーター**
- `taskId` (path, required): タスクのUUID

**レスポンス**
- いいね追加時（200 OK）:
```json
{
  "id": "いいねID",
  "userId": "ユーザーID",
  "targetType": "TASK",
  "targetId": "タスクID",
  "createdAt": "2023-01-01T10:00:00Z"
}
```

- いいね削除時（200 OK）:
```json
null
```

**エラーケース**
- `400 Bad Request`: タスクが見つからない、アクセス権限なし
- `401 Unauthorized`: 認証が必要

### 2. コメントいいねトグル

```
POST /api/comments/{commentId}/like
```

コメントのいいねを追加/削除します（トグル操作）。

**パラメーター**
- `commentId` (path, required): コメントのUUID

**レスポンス**
- いいね追加時（200 OK）:
```json
{
  "id": "いいねID",
  "userId": "ユーザーID", 
  "targetType": "COMMENT",
  "targetId": "コメントID",
  "createdAt": "2023-01-01T10:00:00Z"
}
```

- いいね削除時（200 OK）:
```json
null
```

### 3. いいね削除

```
DELETE /api/likes/{likeId}
```

特定のいいねを削除します。

**パラメーター**
- `likeId` (path, required): いいねのUUID

**レスポンス（204 No Content）**

**エラーケース**
- `400 Bad Request`: いいねが見つからない、他人のいいねを削除しようとした
- `401 Unauthorized`: 認証が必要

---

## フロントエンド実装のポイント

### 1. 認証
- すべてのAPIリクエストで認証クッキー（AT: アクセストークン）を自動送信
- 401エラー時は認証ページにリダイレクト

### 2. コメント表示
- スレッド型コメント（親子関係）をネストして表示
- `parentCommentId`がnullのものが親コメント
- ページングに対応（無限スクロールを推奨）

### 3. いいね機能
- トグル操作（同じAPIを呼び出すだけ）
- レスポンスがnullの場合は「いいね削除」、オブジェクトの場合は「いいね追加」
- UIでいいね数をリアルタイム反映（楽観的更新）

### 4. エラーハンドリング
- 400エラー: エラーメッセージを表示
- 401エラー: ログインページにリダイレクト
- ネットワークエラー: 再試行オプション表示

### 5. UX向上のための実装例

**コメント投稿時**
```javascript
// 楽観的更新
const optimisticComment = {
  id: 'temp-' + Date.now(),
  content: newCommentText,
  likeCount: 0,
  createdAt: new Date().toISOString(),
  authorUsername: currentUser.username,
  authorDisplayName: currentUser.displayName,
  // ... other fields
};

// UIに即座に反映
setComments(prev => [...prev, optimisticComment]);

try {
  // API呼び出し
  const result = await createComment(taskId, { content: newCommentText });
  // 成功時は正式なデータに置き換え
  setComments(prev => prev.map(c => 
    c.id === optimisticComment.id ? result : c
  ));
} catch (error) {
  // 失敗時は楽観的追加を取り消し
  setComments(prev => prev.filter(c => c.id !== optimisticComment.id));
  showErrorMessage('コメント投稿に失敗しました');
}
```

**いいねトグル時**
```javascript
const toggleLike = async (taskId, currentLikeCount, userLiked) => {
  // 楽観的更新
  const newCount = userLiked ? currentLikeCount - 1 : currentLikeCount + 1;
  updateTaskLikeCount(taskId, newCount, !userLiked);

  try {
    const result = await toggleTaskLike(taskId);
    const actuallyLiked = result !== null;
    // 実際の結果で修正（通常は楽観的更新と一致）
    updateTaskLikeCount(taskId, actualLikeCount, actuallyLiked);
  } catch (error) {
    // 失敗時は元に戻す
    updateTaskLikeCount(taskId, currentLikeCount, userLiked);
    showErrorMessage('いいねの更新に失敗しました');
  }
};
```

---

## セキュリティとアクセス制御

### コメント機能
- コメントの閲覧権限はタスクの可視性設定に従う
- コメントの編集・削除は作成者のみ可能

### いいね機能  
- いいねの追加・削除は対象（タスク/コメント）にアクセス可能なユーザーのみ
- いいね数のカウントは自動更新（整合性保証）

### 共通
- すべての操作でJWT認証が必要
- レート制限あり（大量リクエストによるスパム防止）
- 入力値検証（XSS対策、文字数制限など）

---

## 補足事項

- APIレスポンスの日時は全てISO 8601形式（UTC）
- UUIDは全て小文字のハイフン区切り形式
- ページングはSpring Dataの標準形式を採用
- エラーメッセージは本番環境では詳細を制限する場合あり

---

このAPI仕様書に沿って、スレッド型コメント機能といいね機能を持つ、使いやすいタスク管理UIを実装してください。