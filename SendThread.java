import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
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
        this.terminal = TerminalBuilder.builder().system(true).jna(true).encoding(StandardCharsets.UTF_8).build();
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
                        System.out.println("終了します...");
                        socket.close();
                        break;
                    }
                } else if (ch == 127 || ch == 8) {
                    if (inputBuffer.length() > 0) {
                        inputBuffer.deleteCharAt(inputBuffer.length() - 1);
                        // terminal.puts(InfoCmp.Capability.cursor_left);
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
