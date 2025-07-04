import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.TimeZone;
import java.nio.charset.StandardCharsets;
import org.jline.terminal.*;
import org.jline.utils.NonBlockingReader;

public class SendThread extends Thread {
    private Socket socket;
    private final PrintWriter out;
    private final Terminal terminal;

    private volatile long lastTypingTime = 0;
    private static final long TIMEOUT = 3000;
    private boolean wasTyping = true;

    public SendThread(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        this.terminal = TerminalBuilder.builder().system(true).encoding(StandardCharsets.UTF_8).build();

    private List<String> messageHistory;

    public SendThread(Socket socket, List<String> messageHistory) {
        this.socket = socket;
        this.messageHistory = messageHistory;
    }

    @Override
    public void run() {

        NonBlockingReader reader = terminal.reader();

        Thread TypingThread = new Thread(() -> {
            while (!socket.isClosed()) {
                long now = System.currentTimeMillis();
                boolean isTyping = (now - lastTypingTime) < TIMEOUT;

                if (isTyping != wasTyping) {
                    out.println(isTyping ? "入力中…" : "待機中…");
                    wasTyping = isTyping;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {

        try {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

            System.out.print("What is your name? ");
            String name = userInput.readLine();
            out.println(name);

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));

            String line;
            while ((line = userInput.readLine()) != null) {
                if ("list".equalsIgnoreCase(line)) {
                    System.out.println("--- メッセージ履歴 ---");
                    synchronized (messageHistory) {
                        for (String msg : messageHistory) {
                            System.out.println(msg);
                        }
                    }
                    System.out.println("---------------------");
                    continue;
                }

                String timestamp = sdf.format(new Date());
                String message = "You: [" + timestamp + "] " + line;
                out.println("[" + timestamp + "] " + line);

                synchronized (messageHistory) {
                    messageHistory.add(message);
                }

                if ("bye".equalsIgnoreCase(line)) {
                    System.out.println("終了します...");
                    socket.close();
                    break;
                }
            }
        });
        TypingThread.start();

        StringBuilder inputBuffer = new StringBuilder();

        try {
            while (true) {
                int ch = reader.read(100);

                if (ch <= -1) continue;

                else if (ch == 27) {
                    while ((ch = reader.read(100)) > -1) {}
                    continue;
                    // 特殊キーの無視
                    // 矢印キーにはあとで対応予定
                }

                lastTypingTime = System.currentTimeMillis();

                if (ch == 10 || ch == 13) {
                    String line = inputBuffer.toString();
                    inputBuffer.setLength(0);
                    String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
                    out.println("[" + timestamp + "] " + line);
                    terminal.writer().println("\r\u001b[2K自分: [" + timestamp + "] " + line);
                    terminal.writer().print("\r待機中… ");
                    terminal.flush();

                    if ("bye".equalsIgnoreCase(line)) {
                        System.out.println("\r終了します...");
                        socket.close();
                        break;
                    }
                } else if (ch == 127 || ch == 8) {
                    if (inputBuffer.length() > 0) {
                        inputBuffer.deleteCharAt(inputBuffer.length() - 1);
                        terminal.writer().print("\b \b");
                        terminal.flush();
                    }
                } else {
                    inputBuffer.append((char) ch);
                    terminal.writer().print((char) ch);
                    terminal.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("送信エラー: " + e.getMessage());
        } finally {
            try {
                terminal.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
