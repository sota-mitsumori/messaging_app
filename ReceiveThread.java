import java.io.*;
import java.net.Socket;

public class ReceiveThread extends Thread {
    private Socket socket;

    public ReceiveThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("相手: " + line);
            }
        } catch (IOException e) {
            System.out.println("受信エラー: " + e.getMessage());
        }
    }
}