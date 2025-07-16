import { useState, useEffect, useRef, useCallback } from "react";
import { Geist } from "next/font/google";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

// メッセージコンポーネント
const MessageItem = ({ msg, myUsername, onVisible }) => {
  const msgRef = useRef(null);

  const isSelf = msg.sender === myUsername;
  const isSystem = msg.sender === "System";

  useEffect(() => {
    if (msgRef.current && !isSelf) {
      const observer = new IntersectionObserver(
        ([entry]) => {
          if (entry.isIntersecting) {
            onVisible(msg.id);
            observer.disconnect();
          }
        },
        { threshold: 0.8 }
      );
      observer.observe(msgRef.current);
      return () => observer.disconnect();
    }
  }, [msg.id, isSelf, onVisible]);

  if (isSystem) {
    return (
      <div className="text-sm text-gray-500 text-center py-2">
        {msg.text}
      </div>
    );
  }

  const readByCount = msg.readBy.filter(u => u !== myUsername && u !== msg.sender).length;

  return (
    <div ref={msgRef} className={`flex flex-col ${isSelf ? 'items-end' : 'items-start'} mb-4`}>
      <div className="text-xs text-gray-500 mb-1 px-2">{isSelf ? "You" : msg.sender}</div>
      <div className={`max-w-[70%] p-3 rounded-2xl ${isSelf ? 'bg-blue-500 text-white rounded-br-md' : 'bg-gray-200 text-black rounded-bl-md'}`}>
        <p className="text-sm break-words">{msg.text}</p>
      </div>
      <div className="text-xs text-gray-400 mt-1 px-2">{msg.timestamp}</div>
      {isSelf && readByCount > 0 && (
        <div className="text-xs text-blue-600 mt-1 pr-1">
          既読 {readByCount}
        </div>
      )}
    </div>
  );
};


export default function Home() {
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState("");
  const [username, setUsername] = useState("");
  const [isConnected, setIsConnected] = useState(false);
  const [connectedUsers, setConnectedUsers] = useState([]);
  const [typingUsers, setTypingUsers] = useState(new Set());
  const [ws, setWs] = useState(null);
  const messagesEndRef = useRef(null);
  const [showLogin, setShowLogin] = useState(true);
  const typingTimeoutRef = useRef(null);
  const sentReadReceipts = useRef(new Set());

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleInputChange = (e) => {
    setNewMessage(e.target.value);
    if (ws && isConnected) {
      if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
      ws.send("TYPING:true");
      typingTimeoutRef.current = setTimeout(() => {
        if (ws && isConnected) ws.send("TYPING:false");
      }, 3000);
    }
  };

  const connectWebSocket = (user) => {
    const websocket = new WebSocket("ws://localhost:8080");
    
    websocket.onopen = () => {
      console.log("WebSocket connected");
      websocket.send(`CONNECT:${user}`);
    };

    websocket.onmessage = (event) => {
      const message = event.data;
      const [type, ...dataParts] = message.split(/:(.*)/s);
      const data = dataParts[0] || '';

      switch (type) {
        case "CONNECTED":
          console.log("Connected as:", data);
          setIsConnected(true);
          setShowLogin(false);
          break;
        case "NEW_MESSAGE": {
          const parts = data.split('::');
          if (parts.length >= 4) {
            const [id, sender, timestamp, ...textParts] = parts;
            const text = textParts.join('::');
            setMessages(prev => {
              if (prev.some(msg => msg.id === id)) return prev;
              return [...prev, { id, sender, text, timestamp, readBy: [] }];
            });
          }
          break;
        }
        case "READ_UPDATE": {
          const [msgId, readersStr] = data.split('::');
          const readers = readersStr ? readersStr.split(',') : [];
          setMessages(prev =>
            prev.map(msg =>
              msg.id === msgId ? { ...msg, readBy: readers } : msg
            )
          );
          break;
        }
        case "USERS":
          setConnectedUsers(data ? data.split(',').filter(u => u.trim()) : []);
          break;
        case "TYPING": {
          const [typingUser, isTyping] = data.split(",", 2);
          setTypingUsers(prev => {
            const newSet = new Set(prev);
            if (isTyping === "true") newSet.add(typingUser);
            else newSet.delete(typingUser);
            return newSet;
          });
          break;
        }
        default:
          console.log("Unknown message type:", type);
      }
    };

    websocket.onclose = () => {
      console.log("WebSocket disconnected");
      setIsConnected(false);
      setShowLogin(true);
      setWs(null);
    };
    websocket.onerror = (error) => {
      console.error("WebSocket error:", error);
    };
    setWs(websocket);
  };

  const disconnectWebSocket = () => {
    if (ws) {
      console.log("Disconnecting...");
      ws.send(`DISCONNECT:${username}`);
      ws.close();
    }
  };

  const sendMessage = () => {
    if (!newMessage.trim() || !ws) return;
    ws.send(`MESSAGE:${newMessage}`);
    setNewMessage("");
    if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
    ws.send("TYPING:false");
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const connectUser = () => {
    if (!username.trim()) return;
    connectWebSocket(username);
  };

  const handleMessageVisible = useCallback((messageId) => {
    if (ws && !sentReadReceipts.current.has(messageId)) {
      ws.send(`READ:${messageId}`);
      sentReadReceipts.current.add(messageId);
    }
  }, [ws]);

  if (showLogin) {
    return (
      <div className={`${geistSans.variable} font-sans min-h-screen bg-gray-50 flex items-center justify-center`}>
        <div className="bg-white p-8 rounded-lg shadow-md w-full max-w-sm">
          <h1 className="text-2xl font-bold text-center mb-6 text-gray-800">WebSocket Chat</h1>
          <div className="space-y-4">
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && connectUser()}
              className="w-full px-3 py-2 border border-gray-300 text-black rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="あなたの名前を入力"
            />
            <button
              onClick={connectUser}
              disabled={!username.trim()}
              className="w-full bg-blue-500 text-white py-2 px-4 rounded-md hover:bg-blue-600 disabled:bg-gray-300 transition-colors"
            >
              チャットに参加
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={`${geistSans.variable} font-sans min-h-screen bg-gray-100 flex justify-center items-center p-4`}>
      <div className="w-full max-w-4xl h-[90vh] bg-white rounded-lg shadow-xl flex">
        {/* Users Panel */}
        <div className="w-1/4 bg-gray-50 border-r border-gray-200 flex flex-col">
          <div className="p-4 border-b">
            <h2 className="text-lg font-semibold text-gray-800">オンライン</h2>
          </div>
          <div className="flex-1 overflow-y-auto p-4 space-y-3">
            {connectedUsers.map((user, index) => (
              <div key={index} className="flex items-center space-x-3">
                <div className="w-2.5 h-2.5 bg-green-500 rounded-full"></div>
                <span className="text-sm text-gray-700 font-medium">{user}</span>
              </div>
            ))}
          </div>
          <div className="p-4 border-t">
            <button
              onClick={disconnectWebSocket}
              className="w-full bg-red-500 text-white py-2 px-4 rounded-md hover:bg-red-600 transition-colors text-sm"
            >
              退出する
            </button>
          </div>
        </div>

        {/* Chat Area */}
        <div className="w-3/4 flex flex-col">
          <div className="flex-1 overflow-y-auto p-4">
            {messages.map((msg) => (
              <MessageItem key={msg.id} msg={msg} myUsername={username} onVisible={handleMessageVisible} />
            ))}
            <div className="h-8">
              {typingUsers.size > 0 && (
                <div className="text-sm text-gray-500 italic pt-2">
                  {Array.from(typingUsers).join(", ")} が入力中...
                </div>
              )}
            </div>
            <div ref={messagesEndRef} />
          </div>

          {/* Message Input */}
          <div className="border-t p-4 bg-gray-50">
            <div className="flex space-x-3 items-center">
              <textarea
                value={newMessage}
                onChange={handleInputChange}
                onKeyPress={handleKeyPress}
                placeholder="メッセージを入力..."
                className="flex-1 px-4 py-2 border border-gray-300 text-black rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                rows="1"
              />
              <button
                onClick={sendMessage}
                disabled={!newMessage.trim()}
                className="bg-blue-500 text-white w-10 h-10 rounded-full flex items-center justify-center hover:bg-blue-600 disabled:bg-gray-300 transition-colors flex-shrink-0"
              >
                <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20"><path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A1 1 0 009 15.571V11a1 1 0 112 0v4.571a1 1 0 00.725.962l5 1.428a1 1 0 001.17-1.408l-7-14z"></path></svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
