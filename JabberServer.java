import java.io.*;
import java.net.*;

public class JabberServer {
    public static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("サーバー起動、接続待機中...");

        Socket socket = serverSocket.accept();
        System.out.println("クライアント接続: " + socket);

        ReceiveThread receiveThread = new ReceiveThread(socket);
        SendThread sendThread = new SendThread(socket);

        receiveThread.start();
        sendThread.start();
    }
}
