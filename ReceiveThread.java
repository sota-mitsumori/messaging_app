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
            // ★ ソケットの入力ストリームをUTF-8として読み込む
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            String line;
            while ((line = in.readLine()) != null) {
                // ★ 出力側の文字化け対策（念のためPrintStreamにUTF-8指定）
                if (line.startsWith("[")){
                    System.out.println("\r相手: " + line);
                } else {
                    System.out.print("\r" + line + " ");
                }
            }
        } catch (IOException e) {
            System.out.println("受信エラー: " + e.getMessage());
        }
    }
}
