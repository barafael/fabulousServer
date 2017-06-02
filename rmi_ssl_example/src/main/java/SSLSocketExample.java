
/**
 *
 */
import javax.net.ssl.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SSLSocketExample {

    /**
     * @param args
     */
    public static void main(String[] args) {
        Executors.newSingleThreadExecutor().execute(new Server());
        Executors.newSingleThreadExecutor().execute(new Client());
    }

    static class Server implements Runnable {
        public void run() {
            try {
                SSLServerSocket serverSocket = (SSLServerSocket) SSLServerSocketFactory
                        .getDefault().createServerSocket(5679);
                System.out.println("Server ready..." + serverSocket);

                System.out
                        .println("Supported Cipher Suites: "
                                + Arrays
                                .toString(((SSLServerSocketFactory) SSLServerSocketFactory
                                        .getDefault())
                                        .getSupportedCipherSuites()));

                SSLSocket socket = (SSLSocket) serverSocket.accept();
                SSLSession sslSession = socket.getSession();
                String cipherSuite = sslSession.getCipherSuite();
                System.out.println(cipherSuite);

                Scanner scanner = new Scanner(socket.getInputStream());
                System.out.println("Reading...");
                while (scanner.hasNextLine()) {
                    System.out.println("Server received: " + scanner.nextLine());
                }
                scanner.close();

                socket.close();

                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class Client implements Runnable {
        public void run() {
            try {

                SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault()
                        .createSocket("localhost", 5679);
                PrintWriter printWriter = new PrintWriter(socket
                        .getOutputStream());
                System.out.println("Client -> sending...");
                for (int i = 0; i < 100; i++) {
                    String message = "Hallo: " + i;
                    System.out.println("Client sent: " + message);
                    printWriter.println(message);
                    printWriter.flush();
                    TimeUnit.SECONDS.sleep(1);
                }
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}