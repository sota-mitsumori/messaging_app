# Socket Chat Application

ソケット通信を使用したリアルタイムチャットアプリケーション（Javaバックエンド + Next.jsフロントエンド）

## 概要

このアプリケーションは、**ソケット通信の要件を満たしつつ**、Webブラウザからも利用できるチャットシステムです。

- **バックエンド**: Java WebSocketサーバー（ソケット通信）
- **フロントエンド**: Next.js Reactアプリケーション
- **通信方式**: WebSocketプロトコル（ソケットベース）

## アーキテクチャ

### ソケット通信の実装
- **WebSocketプロトコル**: ブラウザとJavaサーバー間のソケット通信
- **リアルタイム通信**: 双方向の即座なメッセージ送受信
- **マルチユーザー対応**: 複数ユーザーが同時にチャットに参加可能

### 技術スタック
- **バックエンド**: Java (WebSocket Server)
- **フロントエンド**: Next.js, React, Tailwind CSS
- **通信**: WebSocket (ws://)

## ファイル構成

```
line/
├── WebSocketServer.java      # メインのWebSocketサーバー
├── JabberServer.java         # 元のソケットベースサーバー（参考用）
├── JabberClient.java         # 元のソケットベースクライアント（参考用）
├── ReceiveThread.java        # 元の受信スレッド（参考用）
├── SendThread.java           # 元の送信スレッド（参考用）
├── frontend/                 # Next.jsフロントエンド
│   ├── pages/
│   │   └── index.js          # メインのチャットUI
│   ├── package.json
│   └── ...
└── README.md
```

## セットアップと実行

### 1. Javaバックエンドの起動

```bash
# Javaファイルをコンパイル
javac WebSocketServer.java

# WebSocketサーバーを起動
java WebSocketServer
```

サーバーが起動すると以下のメッセージが表示されます：
```
WebSocket Server started on port 8080
Waiting for connections...
```

### 2. Next.jsフロントエンドの起動

```bash
# フロントエンドディレクトリに移動
cd frontend

# 依存関係をインストール
npm install

# 開発サーバーを起動
npm run dev
```

フロントエンドが起動すると以下のURLでアクセス可能です：
- ローカル: http://localhost:3000
- ネットワーク: http://[IPアドレス]:3000

## 使用方法

1. **サーバー起動**: JavaバックエンドとNext.jsフロントエンドを起動
2. **ブラウザアクセス**: http://localhost:3000 にアクセス
3. **ユーザー名入力**: チャットに参加するためのユーザー名を入力
4. **チャット開始**: メッセージを入力して送信

## 機能

### チャット機能
- ✅ リアルタイムメッセージ送受信
- ✅ ユーザー参加・退出通知
- ✅ メッセージ履歴表示
- ✅ オンラインユーザー一覧
- ✅ タイムスタンプ付きメッセージ

### ソケット通信の特徴
- ✅ **WebSocketプロトコル**: ブラウザとサーバー間のソケット通信
- ✅ **双方向通信**: クライアント・サーバー間の即座なデータ交換
- ✅ **接続管理**: ユーザーの接続・切断の自動検知
- ✅ **ブロードキャスト**: 全ユーザーへのメッセージ配信

## 技術詳細

### WebSocketサーバー（WebSocketServer.java）

#### 主要機能
- **WebSocketハンドシェイク**: ブラウザとの接続確立
- **メッセージ処理**: テキストフレームの解析と処理
- **ユーザー管理**: 接続ユーザーの追跡
- **メッセージ履歴**: チャット履歴の保存

#### メッセージ形式
```
CONNECT:username          # ユーザー接続
MESSAGE:message_content   # メッセージ送信
DISCONNECT:username       # ユーザー切断
```

#### レスポンス形式
```
CONNECTED:username        # 接続確認
BROADCAST:message         # ブロードキャストメッセージ
MESSAGES:history          # メッセージ履歴
USERS:user_list           # ユーザー一覧
```

### フロントエンド（Next.js）

#### 主要機能
- **WebSocket接続**: サーバーとのリアルタイム通信
- **状態管理**: React hooksによる状態管理
- **UI更新**: リアルタイムでのUI更新
- **エラーハンドリング**: 接続エラーの処理

#### 技術スタック
- **React**: ユーザーインターフェース
- **WebSocket API**: ブラウザのWebSocket API
- **Tailwind CSS**: スタイリング
- **Next.js**: フレームワーク

## ソケット通信の実装詳細

### WebSocketプロトコル
- **RFC 6455準拠**: 標準的なWebSocketプロトコル
- **ハンドシェイク**: HTTPアップグレードによる接続確立
- **フレーム処理**: バイナリフレームの解析と処理
- **マスキング**: クライアントからのマスク処理

### 通信フロー
1. **接続確立**: ブラウザがWebSocketハンドシェイクを実行
2. **ユーザー登録**: ユーザー名を送信して接続を完了
3. **メッセージ交換**: リアルタイムでメッセージを送受信
4. **切断処理**: ユーザーの切断を検知して処理

## 開発・デバッグ

### デバッグ情報
サーバー側とフロントエンド側の両方にデバッグログが実装されています：

#### サーバー側ログ
```
Received WebSocket message: CONNECT:username
Adding message to history: username: [HH:mm:ss] message
Sending WebSocket message: BROADCAST:message
```

#### フロントエンド側ログ（ブラウザコンソール）
```
WebSocket connected
Received message: BROADCAST:username: [HH:mm:ss] message
Adding broadcast message: username: [HH:mm:ss] message
```

### トラブルシューティング

#### よくある問題
1. **ポート競合**: 8080番ポートが使用中の場合は別のポートに変更
2. **CORSエラー**: WebSocketはCORSの制限を受けないため問題なし
3. **接続エラー**: サーバーが起動していることを確認

#### 解決方法
- サーバーログとブラウザコンソールの両方を確認
- ネットワーク接続の確認
- ファイアウォール設定の確認

## 要件対応

### ソケット通信要件
✅ **ソケットを使用したネットワーク通信**: WebSocketプロトコルによるソケット通信を実装
✅ **リアルタイム通信**: 即座のメッセージ送受信
✅ **マルチユーザー対応**: 複数ユーザーの同時接続
✅ **Webブラウザ対応**: ブラウザからのアクセス可能

## 今後の拡張可能性

- **プライベートメッセージ**: 特定ユーザーへの直接メッセージ
- **ファイル共有**: 画像やファイルの送信機能
- **ルーム機能**: 複数のチャットルーム
- **認証システム**: ユーザー認証の実装
- **メッセージ永続化**: データベースへの保存

## ライセンス

このプロジェクトは教育目的で作成されています。
