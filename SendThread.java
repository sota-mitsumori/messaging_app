import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
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
            // ★ System.inからの入力をUTF-8として読み込む
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

            String line;
            while ((line = userInput.readLine()) != null) {
                String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
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
