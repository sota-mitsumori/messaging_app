import java.io.*;
import java.net.*;
import java.util.*;

public class JabberClient {
    public static void main(String[] args) throws IOException {
        InetAddress addr = InetAddress.getByName("localhost");
        Socket socket = new Socket(addr, JabberServer.PORT);
        System.out.println("サーバーに接続: " + socket);

        List<String> messageHistory = new ArrayList<>(); // ★共通履歴

        ReceiveThread receiveThread = new ReceiveThread(socket, messageHistory);
        SendThread sendThread = new SendThread(socket, messageHistory);

        receiveThread.start();
        sendThread.start();
    }
}
