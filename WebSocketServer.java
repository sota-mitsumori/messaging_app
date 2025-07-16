import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSocketServer {
    public static final int PORT = 8080;
    // メッセージ履歴 (Key: messageId, Value: formatted message)
    private static final Map<String, String> messageHistory = new LinkedHashMap<>();
    // 既読状態 (Key: messageId, Value: Set of usernames who have read it)
    private static final Map<String, Set<String>> readStatus = new ConcurrentHashMap<>();
    // 接続中のユーザー
    private static final Set<String> connectedUsers = ConcurrentHashMap.newKeySet();
    // WebSocket接続のコレクション
    private static final Set<WebSocketConnection> connections = ConcurrentHashMap.newKeySet();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    static {
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("WebSocket Server started on port " + PORT);
        System.out.println("Waiting for connections from the Next.js client...");

        while (true) {
            Socket socket = serverSocket.accept();
            WebSocketConnection connection = new WebSocketConnection(socket);
            connections.add(connection);
            connection.start();
        }
    }

    static class WebSocketConnection extends Thread {
        private final Socket socket;
        private InputStream in;
        private OutputStream out;
        private String username;

        public WebSocketConnection(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
                handleHandshake();
                handleCommunication();
            } catch (IOException e) {
                // Client disconnected
            } finally {
                cleanup();
            }
        }

        private void handleHandshake() throws IOException {
            Scanner s = new Scanner(in, "UTF-8");
            String data = s.useDelimiter("\\r\\n\\r\\n").next();
            Matcher get = Pattern.compile("^GET").matcher(data);

            if (get.find()) {
                Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
                if (match.find()) {
                    String key = match.group(1);
                    String acceptKey = generateWebSocketAcceptKey(key);
                    String response =
                        "HTTP/1.1 101 Switching Protocols\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Sec-WebSocket-Accept: " + acceptKey + "\r\n" +
                        "\r\n";
                    out.write(response.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }
            }
        }

        private void handleCommunication() throws IOException {
             while (!socket.isClosed()) {
                int firstByte = in.read();
                if (firstByte == -1) break;

                int opcode = firstByte & 0x0F;
                int secondByte = in.read();
                boolean masked = (secondByte & 0x80) != 0;
                long payloadLength = secondByte & 0x7F;

                if (payloadLength == 126) {
                    payloadLength = ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
                } else if (payloadLength == 127) {
                    payloadLength = 0; // Not supported for simplicity
                    for (int i = 0; i < 8; i++) in.read();
                }

                byte[] mask = new byte[4];
                if (masked) readFully(in, mask);

                byte[] payload = new byte[Math.toIntExact(payloadLength)];
                readFully(in, payload);

                if (masked) {
                    for (int i = 0; i < payload.length; i++) {
                        payload[i] ^= mask[i % 4];
                    }
                }

                switch (opcode) {
                    case 0x1: // Text Frame
                        String message = new String(payload, StandardCharsets.UTF_8);
                        handleWebSocketMessage(message);
                        break;
                    case 0x8: // Close Frame
                        socket.close();
                        break;
                    case 0x9: // Ping Frame
                        sendFrame(0xA, payload); // Respond with Pong
                        break;
                }
            }
        }

        private void handleWebSocketMessage(String message) {
            String[] parts = message.split(":", 2);
            if (parts.length < 2) return;

            String type = parts[0];
            String data = parts[1];

            switch (type) {
                case "CONNECT":
                    this.username = data;
                    connectedUsers.add(username);
                    System.out.println(username + " joined.");
                    sendWebSocketMessage("CONNECTED:" + username);

                    synchronized(messageHistory) {
                        for(Map.Entry<String, String> entry : messageHistory.entrySet()) {
                            sendWebSocketMessage("NEW_MESSAGE:" + entry.getKey() + "::" + entry.getValue());
                        }
                    }
                    synchronized(readStatus) {
                        for(Map.Entry<String, Set<String>> entry : readStatus.entrySet()) {
                            broadcastReadUpdate(entry.getKey(), entry.getValue());
                        }
                    }
                    String joinMsg = username + " joined the chat";
                    broadcastSystemMessage(joinMsg);
                    broadcastUserList();
                    break;

                case "MESSAGE":
                    if (username != null) {
                        String messageId = UUID.randomUUID().toString();
                        String timestamp = sdf.format(new Date());
                        String chatMsg = username + "::" + timestamp + "::" + data;
                        synchronized (messageHistory) {
                            messageHistory.put(messageId, chatMsg);
                        }
                        broadcastMessage("NEW_MESSAGE:" + messageId + "::" + chatMsg);
                    }
                    break;

                case "READ":
                    if (username != null) {
                        String messageId = data;
                        Set<String> readers = readStatus.computeIfAbsent(messageId, k -> ConcurrentHashMap.newKeySet());
                        if (readers.add(username)) {
                            broadcastReadUpdate(messageId, readers);
                        }
                    }
                    break;
                
                case "TYPING": // ★ 入力中機能の追加
                    if (username != null) {
                        for (WebSocketConnection conn : connections) {
                            if (conn != this) { // 自分以外に通知
                                conn.sendWebSocketMessage("TYPING:" + username + "," + data);
                            }
                        }
                    }
                    break;

                case "DISCONNECT":
                    cleanup();
                    break;
            }
        }
        
        private void cleanup() {
            if (username != null) {
                connectedUsers.remove(username);
                System.out.println(username + " left.");
                String leaveMsg = "System: [" + sdf.format(new Date()) + "] " + username + " left the chat";
                broadcastSystemMessage(leaveMsg);
                broadcastUserList();
                username = null;
            }
            connections.remove(this);
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) { /* ignore */ }
        }

        // --- Utility methods for sending frames and messages ---
        
        private void readFully(InputStream is, byte[] buffer) throws IOException {
            int offset = 0;
            while (offset < buffer.length) {
                int bytesRead = is.read(buffer, offset, buffer.length - offset);
                if (bytesRead == -1) throw new IOException("Unexpected end of stream");
                offset += bytesRead;
            }
        }

        private synchronized void sendFrame(int opcode, byte[] payload) throws IOException {
            if (socket.isClosed()) return;
            out.write(0x80 | opcode);
            if (payload.length < 126) {
                out.write(payload.length);
            } else { 
                out.write(126);
                out.write((payload.length >> 8) & 0xFF);
                out.write(payload.length & 0xFF);
            }
            out.write(payload);
            out.flush();
        }

        private void sendWebSocketMessage(String message) {
            try {
                sendFrame(0x1, message.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                cleanup();
            }
        }
        
        private void broadcastSystemMessage(String text) {
            String messageId = UUID.randomUUID().toString();
            String timestamp = sdf.format(new Date());
            String systemMsg = "System::" + timestamp + "::" + text;
            synchronized (messageHistory) {
                messageHistory.put(messageId, systemMsg);
            }
            broadcastMessage("NEW_MESSAGE:" + messageId + "::" + systemMsg);
        }

        private void broadcastMessage(String message) {
            for (WebSocketConnection conn : connections) {
                conn.sendWebSocketMessage(message);
            }
        }

        private void broadcastUserList() {
            String userListStr = String.join(",", connectedUsers);
            broadcastMessage("USERS:" + userListStr);
        }

        private void broadcastReadUpdate(String messageId, Set<String> readers) {
            String readerListStr = String.join(",", readers);
            broadcastMessage("READ_UPDATE:" + messageId + "::" + readerListStr);
        }

        private String generateWebSocketAcceptKey(String key) {
            try {
                String guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                byte[] hash = md.digest((key + guid).getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(hash);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
