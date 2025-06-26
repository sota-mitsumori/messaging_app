import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ReceiveThread extends Thread {
    private Socket socket;
    private List<String> messageHistory;

    public ReceiveThread(Socket socket, List<String> messageHistory) {
        this.socket = socket;
        this.messageHistory = messageHistory;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));


            // ★ 最初に相手の名前を受信

            String partnerName = in.readLine();
            if (partnerName == null) {
                System.out.println("名前が受信できませんでした。接続終了。");
                return;
            }

            String line;
            while ((line = in.readLine()) != null) {
                String msg = partnerName + ": " + line;
                System.out.println(msg);
                synchronized (messageHistory) {
                    messageHistory.add(msg);
                }
            //System.out.println("相手の名前: " + partnerName);

            }
        } catch (IOException e) {
            System.out.println("受信エラー: " + e.getMessage());
        }
    }
}
