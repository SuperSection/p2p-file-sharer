package com.supersection.p2p.services;

import com.supersection.p2p.utils.UploadUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class FileSharer {

    private final HashMap<Integer, String> availableFiles;

    public FileSharer() {
        availableFiles = new HashMap<>();
    }

    public int offerFile(String filePath) {
        int port;
        while (true) {
            port = UploadUtils.generateCode();
            if (!availableFiles.containsKey(port)) {
                availableFiles.put(port, filePath);
                return port;
            }
        }
    }

    public void startFileServer(int port) {
        String filePath = availableFiles.get(port);
        if (filePath == null) {
            System.out.println("No file is associated with port: " + port);
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serving file " + new File(filePath).getName() + " on port " + port);
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connection: " + clientSocket.getInetAddress());

            new Thread(new FileSenderHandler(clientSocket, filePath));

        } catch (IOException e) {
            System.err.println("Error handling file server on port: "+ port);
        }
    }

    private record FileSenderHandler(Socket clientSocket, String filePath) implements Runnable {

        @Override
        public void run() {
            try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
                OutputStream outputStream = clientSocket.getOutputStream();
                String filename = new File(filePath).getName();

                String header = "Filename: " + filename + "\n";
                outputStream.write(header.getBytes());

                byte[] buffer = new byte[4096];
                int byteRead;

                while ((byteRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, byteRead);
                }
                System.out.println("file " + filename + " sent to  " + clientSocket.getInetAddress());

            } catch (FileNotFoundException e) {
                System.err.println("Failed to get the file: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Error sending file to the client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (Exception e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
            }
        }
    }

}
