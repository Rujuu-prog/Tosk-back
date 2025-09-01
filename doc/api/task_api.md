# Task API 仕様書（フロントエンド向け）

このドキュメントは、Toskアプリケーションのタスク機能APIの実装ガイドです。

## 概要

Task APIは、ユーザーのタスク管理機能を提供します。タスクの作成、読み取り、更新、削除（CRUD）操作に加えて、検索・フィルタリング機能も提供しています。

### 基本情報
- **ベースURL**: `/api/tasks`
- **認証**: JWT Bearer Token必須
- **Content-Type**: `application/json`

## エンドポイント一覧

### 1. タスク作成
```
POST /api/tasks
```

新しいタスクを作成します。

**リクエスト例:**
```javascript
const response = await fetch('/api/tasks', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token
  },
  body: JSON.stringify({
    title: "新しいタスク",
    description: "タスクの詳細説明",
    dueDate: "2025-12-31",  // YYYY-MM-DD形式
    teamId: "123e4567-e89b-12d3-a456-426614174000", // 任意
    priority: "HIGH",       // LOW, MEDIUM, HIGH
    visibility: "PRIVATE"   // PRIVATE, TEAM, PUBLIC
  })
});
```

**レスポンス例:**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "userId": "user-uuid",
  "teamId": "team-uuid",
  "title": "新しいタスク",
  "description": "タスクの詳細説明",
  "dueDate": "2025-12-31",
  "priority": "HIGH",
  "visibility": "PRIVATE",
  "likeCount": 0,
  "commentCount": 0,
  "createdAt": "2025-08-31T17:00:00Z",
  "updatedAt": "2025-08-31T17:00:00Z"
}
```

**バリデーション:**
- `title`: 必須、1-120文字
- `description`: 任意、最大10,000文字
- `dueDate`: 任意、YYYY-MM-DD形式
- `priority`: 必須、LOW/MEDIUM/HIGH
- `visibility`: 必須、PRIVATE/TEAM/PUBLIC

### 2. タスク取得（単体）
```
GET /api/tasks/{taskId}
```

指定されたIDのタスクを取得します。

**リクエスト例:**
```javascript
const response = await fetch(`/api/tasks/${taskId}`, {
  headers: {
    'Authorization': 'Bearer ' + token
  }
});
const task = await response.json();
```

### 3. タスク一覧取得・検索
```
GET /api/tasks
```

タスク一覧を取得します。複数の検索・フィルタリングオプションが利用できます。

**クエリパラメータ:**
- `keyword`: 検索キーワード（タイトル・説明文を対象）
- `priority`: 優先度フィルタ（LOW, MEDIUM, HIGH）
- `visibility`: 公開設定フィルタ（PRIVATE, TEAM, PUBLIC）
- `dueDateStart`: 期限開始日（YYYY-MM-DD）
- `dueDateEnd`: 期限終了日（YYYY-MM-DD）
- `teamId`: チームIDフィルタ
- `myTasksOnly`: 自分のタスクのみ（true/false）
- `page`: ページ番号（0から開始、デフォルト: 0）
- `size`: ページサイズ（デフォルト: 20）

**リクエスト例:**
```javascript
// 基本的な一覧取得
const response = await fetch('/api/tasks?page=0&size=10', {
  headers: { 'Authorization': 'Bearer ' + token }
});

// 検索・フィルタリング
const searchResponse = await fetch('/api/tasks?' + 
  'keyword=プロジェクト&' +
  'priority=HIGH&' +
  'dueDateStart=2025-01-01&' +
  'dueDateEnd=2025-12-31&' +
  'myTasksOnly=true', {
  headers: { 'Authorization': 'Bearer ' + token }
});
```

**レスポンス例:**
```json
{
  "content": [
    {
      "id": "task-uuid-1",
      "userId": "user-uuid",
      "title": "タスク1",
      "description": "説明1",
      "dueDate": "2025-09-15",
      "priority": "HIGH",
      "visibility": "PRIVATE",
      "likeCount": 5,
      "commentCount": 3,
      "createdAt": "2025-08-31T17:00:00Z",
      "updatedAt": "2025-08-31T17:00:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 25,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

### 4. タスク更新
```
PUT /api/tasks/{taskId}
```

既存のタスクを更新します。

**リクエスト例:**
```javascript
const response = await fetch(`/api/tasks/${taskId}`, {
  method: 'PUT',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token
  },
  body: JSON.stringify({
    title: "更新されたタスク",
    description: "新しい説明",
    dueDate: "2025-12-25",
    priority: "MEDIUM",
    visibility: "TEAM"
  })
});
```

### 5. タスク削除
```
DELETE /api/tasks/{taskId}
```

指定されたタスクを削除します（ソフト削除）。

**リクエスト例:**
```javascript
const response = await fetch(`/api/tasks/${taskId}`, {
  method: 'DELETE',
  headers: {
    'Authorization': 'Bearer ' + token
  }
});
// 204 No Contentが返される
```

## データ型定義

### Priority（優先度）
- `LOW`: 低優先度
- `MEDIUM`: 中優先度（デフォルト）
- `HIGH`: 高優先度

### Visibility（公開設定）
- `PRIVATE`: 自分のみ閲覧可能
- `TEAM`: チームメンバーが閲覧可能
- `PUBLIC`: 全ユーザーが閲覧可能

## 実装例

### React Hooks での使用例

```jsx
import { useState, useEffect } from 'react';

// タスク一覧取得フック
export const useTasks = (filters = {}) => {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchTasks = async () => {
      try {
        const params = new URLSearchParams(filters);
        const response = await fetch(`/api/tasks?${params}`, {
          headers: { 'Authorization': 'Bearer ' + getToken() }
        });
        const data = await response.json();
        setTasks(data.content);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchTasks();
  }, [filters]);

  return { tasks, loading, error };
};

// タスク作成フック
export const useCreateTask = () => {
  const [creating, setCreating] = useState(false);

  const createTask = async (taskData) => {
    setCreating(true);
    try {
      const response = await fetch('/api/tasks', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + getToken()
        },
        body: JSON.stringify(taskData)
      });
      return await response.json();
    } finally {
      setCreating(false);
    }
  };

  return { createTask, creating };
};
```

### TaskListコンポーネントの例

```jsx
import React from 'react';
import { useTasks } from './hooks/useTasks';

const TaskList = ({ filters }) => {
  const { tasks, loading, error } = useTasks(filters);

  if (loading) return <div>読み込み中...</div>;
  if (error) return <div>エラー: {error}</div>;

  return (
    <div className="task-list">
      {tasks.map(task => (
        <TaskCard key={task.id} task={task} />
      ))}
    </div>
  );
};

const TaskCard = ({ task }) => (
  <div className="task-card">
    <h3>{task.title}</h3>
    <p>{task.description}</p>
    <div className="task-meta">
      <span className={`priority priority-${task.priority.toLowerCase()}`}>
        {task.priority}
      </span>
      <span className="due-date">期限: {task.dueDate}</span>
      <span className="stats">
        👍 {task.likeCount} 💬 {task.commentCount}
      </span>
    </div>
  </div>
);
```

### 検索・フィルタリング機能の実装例

```jsx
const TaskSearch = ({ onFiltersChange }) => {
  const [filters, setFilters] = useState({
    keyword: '',
    priority: '',
    visibility: '',
    myTasksOnly: false
  });

  const handleFilterChange = (key, value) => {
    const newFilters = { ...filters, [key]: value };
    setFilters(newFilters);
    onFiltersChange(newFilters);
  };

  return (
    <div className="task-search">
      <input
        type="text"
        placeholder="キーワード検索"
        value={filters.keyword}
        onChange={(e) => handleFilterChange('keyword', e.target.value)}
      />
      
      <select
        value={filters.priority}
        onChange={(e) => handleFilterChange('priority', e.target.value)}
      >
        <option value="">全ての優先度</option>
        <option value="HIGH">高</option>
        <option value="MEDIUM">中</option>
        <option value="LOW">低</option>
      </select>

      <label>
        <input
          type="checkbox"
          checked={filters.myTasksOnly}
          onChange={(e) => handleFilterChange('myTasksOnly', e.target.checked)}
        />
        自分のタスクのみ
      </label>
    </div>
  );
};
```

## エラーハンドリング

### 一般的なHTTPステータスコード

- `200 OK`: 正常処理
- `201 Created`: 作成成功
- `204 No Content`: 削除成功
- `400 Bad Request`: リクエストエラー
- `401 Unauthorized`: 認証エラー
- `403 Forbidden`: 権限エラー
- `404 Not Found`: リソース未発見
- `422 Unprocessable Entity`: バリデーションエラー

### エラーレスポンス例

```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "タイトルは必須です",
  "details": {
    "field": "title",
    "code": "NotBlank"
  }
}
```

### エラーハンドリング実装例

```javascript
const handleApiError = (response) => {
  switch (response.status) {
    case 401:
      // 認証エラー - ログインページへリダイレクト
      window.location.href = '/login';
      break;
    case 403:
      // 権限エラー
      alert('このタスクにアクセスする権限がありません');
      break;
    case 422:
      // バリデーションエラー
      return response.json().then(error => {
        throw new Error(error.message);
      });
    default:
      throw new Error('予期しないエラーが発生しました');
  }
};
```

## パフォーマンス最適化

### ページネーション

大量のタスクを効率的に表示するため、ページネーションを使用してください：

```javascript
const TaskPagination = ({ currentPage, totalPages, onPageChange }) => {
  return (
    <div className="pagination">
      <button
        disabled={currentPage === 0}
        onClick={() => onPageChange(currentPage - 1)}
      >
        前へ
      </button>
      
      <span>{currentPage + 1} / {totalPages}</span>
      
      <button
        disabled={currentPage >= totalPages - 1}
        onClick={() => onPageChange(currentPage + 1)}
      >
        次へ
      </button>
    </div>
  );
};
```

### キャッシング

React Queryなどを使用してAPIレスポンスをキャッシュすることを推奨します：

```javascript
import { useQuery } from 'react-query';

const useTask = (taskId) => {
  return useQuery(['task', taskId], () =>
    fetch(`/api/tasks/${taskId}`, {
      headers: { 'Authorization': 'Bearer ' + getToken() }
    }).then(res => res.json()),
    {
      staleTime: 5 * 60 * 1000, // 5分間キャッシュ
    }
  );
};
```

## テスト例

### Jest + React Testing Library

```javascript
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TaskList } from './TaskList';

// APIモック
jest.mock('./api', () => ({
  fetchTasks: jest.fn(() => Promise.resolve({
    content: [
      {
        id: '1',
        title: 'テストタスク',
        priority: 'HIGH',
        likeCount: 0,
        commentCount: 0
      }
    ],
    totalElements: 1
  }))
}));

test('タスク一覧が正しく表示される', async () => {
  render(<TaskList />);
  
  await waitFor(() => {
    expect(screen.getByText('テストタスク')).toBeInTheDocument();
  });
});
```

## セキュリティ考慮事項

1. **認証トークン**: 全てのAPIリクエストにJWT Bearer Tokenを含める
2. **XSS対策**: ユーザー入力は適切にサニタイズする
3. **CSRF対策**: APIはStatelessなので基本的にCSRF攻撃の心配はない
4. **権限チェック**: タスクの閲覧・編集権限はサーバーサイドで制御される

このAPIを使用することで、包括的なタスク管理機能をフロントエンドに実装できます。