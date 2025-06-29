import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.security.MessageDigest;
import java.util.Base64;

public class WebSocketServer {
    public static final int PORT = 8080;
    private static List<String> messageHistory = new ArrayList<>();
    private static Set<String> connectedUsers = ConcurrentHashMap.newKeySet();
    private static Set<WebSocketConnection> connections = ConcurrentHashMap.newKeySet();
    private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    
    static {
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("WebSocket Server started on port " + PORT);
        System.out.println("Waiting for connections...");

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("New connection: " + socket);
            
            WebSocketConnection connection = new WebSocketConnection(socket);
            connections.add(connection);
            connection.start();
        }
    }
    
    static class WebSocketConnection extends Thread {
        private Socket socket;
        private BufferedReader in;
        private OutputStream out;
        private String username;
        private boolean isWebSocket = false;

        public WebSocketConnection(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = socket.getOutputStream();

                // Read the first line to determine if it's a WebSocket handshake or regular client
                String firstLine = in.readLine();
                if (firstLine != null && firstLine.startsWith("GET")) {
                    // This is a WebSocket handshake
                    handleWebSocketHandshake(firstLine);
                } else {
                    // This is a regular socket client (original JabberClient)
                    // ここではWebSocketのみ対応
                    socket.close();
                }
            } catch (IOException e) {
                System.out.println("Connection error: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void handleWebSocketHandshake(String firstLine) throws IOException {
            isWebSocket = true;
            System.out.println("WebSocket handshake detected");

            // Read all headers
            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    headers.put(parts[0].trim().toLowerCase(), parts[1].trim());
                }
            }

            // Send WebSocket handshake response
            String key = headers.get("sec-websocket-key");
            if (key != null) {
                String acceptKey = generateWebSocketAcceptKey(key);
                String response =
                    "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: " + acceptKey + "\r\n" +
                    "\r\n";
                out.write(response.getBytes(StandardCharsets.UTF_8));
                out.flush();

                // Start WebSocket communication
                handleWebSocketCommunication();
            }
        }

        private void handleWebSocketCommunication() throws IOException {
            InputStream is = socket.getInputStream();
            while (true) {
                int firstByte = is.read();
                if (firstByte == -1) break;

                int opcode = firstByte & 0x0F;
                if (opcode == 0x8) { // Close frame
                    break;
                }
                if (opcode == 0x1) { // Text frame
                    int secondByte = is.read();
                    boolean masked = (secondByte & 0x80) != 0;
                    int payloadLength = secondByte & 0x7F;
                    if (payloadLength == 126) {
                        payloadLength = (is.read() << 8) | is.read();
                    } else if (payloadLength == 127) {
                        for (int i = 0; i < 8; i++) is.read(); // skip
                        payloadLength = 0; // not supported
                    }
                    byte[] mask = new byte[4];
                    if (masked) {
                        is.read(mask);
                    }
                    byte[] payload = new byte[payloadLength];
                    is.read(payload);
                    if (masked) {
                        for (int i = 0; i < payload.length; i++) {
                            payload[i] ^= mask[i % 4];
                        }
                    }
                    String message = new String(payload, StandardCharsets.UTF_8);
                    handleWebSocketMessage(message);
                }
            }
        }

        private void handleWebSocketMessage(String message) {
            System.out.println("Received WebSocket message: " + message);
            // Format: "type:data"
            String[] parts = message.split(":", 2);
            if (parts.length == 2) {
                String type = parts[0];
                String data = parts[1];
                switch (type) {
                    case "CONNECT":
                        username = data;
                        connectedUsers.add(username);
                        String joinMsg = "System: [" + sdf.format(new Date()) + "] " + username + " joined the chat";
                        synchronized (messageHistory) { messageHistory.add(joinMsg); }
                        broadcastMessage(joinMsg);
                        sendWebSocketMessage("CONNECTED:" + username);
                        sendWebSocketMessage("MESSAGES:" + String.join("\n", messageHistory));
                        sendWebSocketMessage("USERS:" + String.join(",", connectedUsers));
                        
                        // 全員にユーザーリストの更新を送信
                        for (WebSocketConnection conn : connections) {
                            if (conn.isWebSocket) {
                                conn.sendWebSocketMessage("USERS:" + String.join(",", connectedUsers));
                            }
                        }
                        break;
                    case "MESSAGE":
                        if (username != null) {
                            String chatMsg = username + ": [" + sdf.format(new Date()) + "] " + data;
                            System.out.println("Adding message to history: " + chatMsg);
                            synchronized (messageHistory) { messageHistory.add(chatMsg); }
                            // 全員（自分を含む）にメッセージを送信
                            for (WebSocketConnection conn : connections) {
                                if (conn.isWebSocket) {
                                    conn.sendWebSocketMessage("BROADCAST:" + chatMsg);
                                }
                            }
                        }
                        break;
                    case "DISCONNECT":
                        if (username != null) {
                            connectedUsers.remove(username);
                            String leaveMsg = "System: [" + sdf.format(new Date()) + "] " + username + " left the chat";
                            synchronized (messageHistory) { messageHistory.add(leaveMsg); }
                            broadcastMessage(leaveMsg);
                            
                            // 全員にユーザーリストの更新を送信
                            for (WebSocketConnection conn : connections) {
                                if (conn.isWebSocket) {
                                    conn.sendWebSocketMessage("USERS:" + String.join(",", connectedUsers));
                                }
                            }
                        }
                        break;
                }
            }
        }

        private void sendWebSocketMessage(String message) {
            try {
                System.out.println("Sending WebSocket message: " + message);
                byte[] payload = message.getBytes(StandardCharsets.UTF_8);
                int length = payload.length;
                out.write(0x81); // FIN + text frame
                if (length < 126) {
                    out.write(length);
                } else if (length < 65536) {
                    out.write(126);
                    out.write((length >> 8) & 0xFF);
                    out.write(length & 0xFF);
                } else {
                    out.write(127);
                    for (int i = 7; i >= 0; i--) {
                        out.write((int)((length >> (i * 8)) & 0xFF));
                    }
                }
                out.write(payload);
                out.flush();
            } catch (IOException e) {
                System.out.println("Error sending WebSocket message: " + e.getMessage());
            }
        }

        private void broadcastMessage(String message) {
            System.out.println("Broadcasting message: " + message);
            for (WebSocketConnection conn : connections) {
                if (conn.isWebSocket) {
                    conn.sendWebSocketMessage("BROADCAST:" + message);
                }
            }
        }

        private void cleanup() {
            if (username != null) {
                connectedUsers.remove(username);
                String leaveMsg = "System: [" + sdf.format(new Date()) + "] " + username + " left the chat";
                synchronized (messageHistory) { messageHistory.add(leaveMsg); }
                broadcastMessage(leaveMsg);
                
                // 全員にユーザーリストの更新を送信
                for (WebSocketConnection conn : connections) {
                    if (conn.isWebSocket) {
                        conn.sendWebSocketMessage("USERS:" + String.join(",", connectedUsers));
                    }
                }
            }
            connections.remove(this);
            try { if (socket != null && !socket.isClosed()) { socket.close(); } } catch (IOException e) {}
        }

        private String generateWebSocketAcceptKey(String key) {
            try {
                String acceptSeed = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                byte[] hash = md.digest(acceptSeed.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(hash);
            } catch (Exception e) {
                return "";
            }
        }
    }
} 