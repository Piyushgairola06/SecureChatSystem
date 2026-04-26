package client;

import java.io.*;
import java.net.Socket;

public class FileReceiver {

    public static void receiveFile(Socket socket, String fileName) {

        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            FileOutputStream fos = new FileOutputStream(fileName);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = dis.read(buffer)) > 0) {
                fos.write(buffer, 0, bytesRead);
            }

            fos.close();
            dis.close();

            System.out.println("File received: " + fileName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
