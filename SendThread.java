import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.TimeZone;
import java.nio.charset.StandardCharsets;

public class SendThread extends Thread {
    private Socket socket;
    private List<String> messageHistory;

    public SendThread(Socket socket, List<String> messageHistory) {
        this.socket = socket;
        this.messageHistory = messageHistory;
    }

    @Override
    public void run() {
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
        } catch (IOException e) {
            System.out.println("送信エラー: " + e.getMessage());
        }
    }
}
