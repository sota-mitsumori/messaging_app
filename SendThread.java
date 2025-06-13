import java.io.*;
import java.net.Socket;

public class SendThread extends Thread {
    private Socket socket;

    public SendThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = userInput.readLine()) != null) {
                out.println(line);
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