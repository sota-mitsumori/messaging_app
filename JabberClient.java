import java.io.*;
import java.net.*;
//JabberClient.java
public class JabberClient {
    public static void main(String[] args) throws IOException {
        InetAddress addr = InetAddress.getByName("localhost");
        Socket socket = new Socket(addr, JabberServer.PORT);
        System.out.println("サーバーに接続: " + socket);

        ReceiveThread receiveThread = new ReceiveThread(socket);
        SendThread sendThread = new SendThread(socket);

        receiveThread.start();
        sendThread.start();

        // スレッド終了待ちは省略
    }
}
