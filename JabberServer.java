import java.io.*;
import java.net.*;
<<<<<<< add-履歴
import java.util.*;
=======
>>>>>>> main

public class JabberServer {
    public static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("サーバー起動、接続待機中...");

        Socket socket = serverSocket.accept();
        System.out.println("クライアント接続: " + socket);

        List<String> messageHistory = new ArrayList<>(); // ★共通履歴

        ReceiveThread receiveThread = new ReceiveThread(socket, messageHistory);
        SendThread sendThread = new SendThread(socket, messageHistory);

        receiveThread.start();
        sendThread.start();
    }
}
