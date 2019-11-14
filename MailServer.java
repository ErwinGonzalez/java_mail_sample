
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

class MailServer{
    public static void main (String[] args) throws Exception{
        int serverPort = 9001;
        ServerSocket serverSocket = new ServerSocket(serverPort);
        ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        System.out.println("Waiting for connections on server port "+serverPort);
        try {
            while (true) {
                tpe.submit(new MailServerThread(serverSocket.accept()));
            }
        }finally {
            serverSocket.close();
        }
    }
}