import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.nio.charset.StandardCharsets;

public class SendThread extends Thread {
    private Socket socket;

    public SendThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

            // ★ 名前を入力して最初に送信
            System.out.print("What is your name? ");
            String name = userInput.readLine();
            out.println(name);  // 名前だけを1行目に送信

            // ★ タイムゾーンを明示的にTokyoに設定
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));

            String line;
            while ((line = userInput.readLine()) != null) {
                String timestamp = sdf.format(new Date());
                out.println("[" + timestamp + "] " + line);

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
