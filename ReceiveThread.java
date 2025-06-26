import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ReceiveThread extends Thread {
    private Socket socket;

    public ReceiveThread(Socket socket) {
        this.socket = socket;
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
            //System.out.println("相手の名前: " + partnerName);

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(partnerName + ": " + line);
            }
        } catch (IOException e) {
            System.out.println("受信エラー: " + e.getMessage());
        }
    }
}
