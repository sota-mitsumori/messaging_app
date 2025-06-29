import { useState, useEffect, useRef } from "react";
import { Geist, Geist_Mono } from "next/font/google";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

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

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // 入力中の状態を管理
  const handleInputChange = (e) => {
    setNewMessage(e.target.value);
    
    // 入力中メッセージを送信
    if (ws && isConnected) {
      ws.send("TYPING:true");
      
      // 既存のタイマーをクリア
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
      
      // 3秒後に「入力中」を停止
      typingTimeoutRef.current = setTimeout(() => {
        if (ws && isConnected) {
          ws.send("TYPING:false");
        }
      }, 3000);
    }
  };

  const connectWebSocket = (username) => {
    const websocket = new window.WebSocket("ws://localhost:8080");
    websocket.onopen = () => {
      console.log("WebSocket connected");
      websocket.send(`CONNECT:${username}`);
    };
    websocket.onmessage = (event) => {
      console.log("Received message:", event.data);
      const message = event.data;
      
      // BROADCASTメッセージの場合は特別な処理
      if (message.startsWith("BROADCAST:")) {
        const data = message.substring(10); // "BROADCAST:" の後ろを取得
        console.log("Adding broadcast message:", data);
        setMessages(prev => {
          console.log("Previous messages:", prev);
          const newMessages = [...prev, data];
          console.log("New messages:", newMessages);
          return newMessages;
        });
        return;
      }
      
      // その他のメッセージは通常通り処理
      const [type, data] = message.split(":", 2);
      console.log("Parsed message - type:", type, "data:", data);
      
      switch (type) {
        case "CONNECTED":
          console.log("Connected as:", data);
          setIsConnected(true);
          setShowLogin(false);
          break;
        case "MESSAGES":
          if (data) {
            const messageList = data.split('\n').filter(msg => msg.trim());
            console.log("Setting messages:", messageList);
            setMessages(messageList);
          }
          break;
        case "USERS":
          if (data) {
            const userList = data.split(',').filter(user => user.trim());
            console.log("Setting users:", userList);
            setConnectedUsers(userList);
          }
          break;
        case "TYPING":
          if (data) {
            const [typingUser, isTyping] = data.split(",", 2);
            setTypingUsers(prev => {
              const newSet = new Set(prev);
              if (isTyping === "true") {
                newSet.add(typingUser);
              } else {
                newSet.delete(typingUser);
              }
              return newSet;
            });
          }
          break;
        default:
          console.log("Unknown message type:", type);
      }
    };
    websocket.onclose = () => {
      console.log("WebSocket disconnected");
      setIsConnected(false);
      setShowLogin(true);
      setUsername("");
      setMessages([]);
      setConnectedUsers([]);
      setTypingUsers(new Set());
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
    console.log("Sending message:", newMessage);
    ws.send(`MESSAGE:${newMessage}`);
    setNewMessage("");
    
    // メッセージ送信時に「入力中」を停止
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }
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

  if (showLogin) {
    return (
      <div className={`${geistSans.className} min-h-screen bg-gray-50 flex items-center justify-center`}>
        <div className="bg-white p-8 rounded-lg shadow-md w-96">
          <h1 className="text-2xl font-bold text-center mb-6 text-gray-800">Socket Chat App</h1>
          <div className="space-y-4">
            <div>
              <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-2">
                Enter your username
              </label>
              <input
                type="text"
                id="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && connectUser()}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Your username"
              />
            </div>
            <button
              onClick={connectUser}
              disabled={!username.trim()}
              className="w-full bg-blue-500 text-white py-2 px-4 rounded-md hover:bg-blue-600 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
            >
              Join Chat (WebSocket)
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={`${geistSans.className} min-h-screen bg-gray-50`}>
      <div className="max-w-4xl mx-auto p-4">
        {/* Header */}
        <div className="bg-white rounded-lg shadow-md p-4 mb-4">
          <div className="flex justify-between items-center">
            <h1 className="text-xl font-bold text-gray-800">Socket Chat Room</h1>
            <div className="flex items-center space-x-4">
              <div className="text-sm text-gray-600">
                Connected as: <span className="font-semibold text-blue-600">{username}</span>
              </div>
              <div className="text-sm text-gray-600">
                Online: <span className="font-semibold text-green-600">{connectedUsers.length}</span>
              </div>
              <div className="text-sm text-gray-600">
                Protocol: <span className="font-semibold text-purple-600">WebSocket</span>
              </div>
              <button
                onClick={disconnectWebSocket}
                className="bg-red-500 text-white px-3 py-1 rounded-md hover:bg-red-600 transition-colors text-sm"
              >
                Disconnect
              </button>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
          {/* Users Panel */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow-md p-4">
              <h2 className="text-lg font-semibold mb-3 text-gray-800">Online Users</h2>
              <div className="space-y-2">
                {connectedUsers.map((user, index) => (
                  <div key={index} className="flex items-center space-x-2">
                    <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                    <span className="text-sm text-gray-700">{user}</span>
                  </div>
                ))}
                {connectedUsers.length === 0 && (
                  <p className="text-sm text-gray-500">No users online</p>
                )}
              </div>
            </div>
          </div>

          {/* Chat Area */}
          <div className="lg:col-span-3">
            <div className="bg-white rounded-lg shadow-md flex flex-col h-96">
              {/* Messages */}
              <div className="flex-1 overflow-y-auto p-4 space-y-2">
                {console.log("Rendering messages:", messages)}
                {messages.length === 0 && (
                  <div className="text-sm text-gray-500 text-center py-4">
                    No messages yet. Start chatting!
                  </div>
                )}
                {messages.map((message, index) => (
                  <div key={index} className="text-sm">
                    <span className="text-gray-600">{message}</span>
                  </div>
                ))}
                
                {/* 入力中表示 */}
                {typingUsers.size > 0 && (
                  <div className="text-sm text-gray-500 italic">
                    {Array.from(typingUsers).join(", ")} {typingUsers.size === 1 ? "is" : "are"} typing...
                  </div>
                )}
                
                <div ref={messagesEndRef} />
              </div>

              {/* Message Input */}
              <div className="border-t p-4">
                <div className="flex space-x-2">
                  <textarea
                    value={newMessage}
                    onChange={handleInputChange}
                    onKeyPress={handleKeyPress}
                    placeholder="Type your message..."
                    className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                    rows="2"
                  />
                  <button
                    onClick={sendMessage}
                    disabled={!newMessage.trim()}
                    className="bg-blue-500 text-white px-4 py-2 rounded-md hover:bg-blue-600 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
                  >
                    Send
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
