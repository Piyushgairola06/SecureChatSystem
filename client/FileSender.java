package client;

import java.io.*;
import java.net.Socket;

public class FileSender {

    public static void sendFile(Socket socket, String filePath) {

        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, bytesRead);
            }

            fis.close();
            dos.close();

            System.out.println("File sent: " + file.getName());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
