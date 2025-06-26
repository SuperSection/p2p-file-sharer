package com.supersection.p2p.controllers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.supersection.p2p.services.FileSharer;
import com.supersection.p2p.utils.Multiparser;
import com.supersection.p2p.utils.ParseResult;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileController {

    private final FileSharer fileSharer;
    private final HttpServer server;
    private final String uploadDir;
    private final ExecutorService executorService;

    public FileController(int port) throws IOException {
        this.fileSharer = new FileSharer();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.uploadDir = System.getProperty("java.io.tmpdir") + File.separator + "p2p-uploads";
        this.executorService = Executors.newFixedThreadPool(10);

        File uploadDirFile = new File(uploadDir);
        if (!uploadDirFile.exists()) uploadDirFile.mkdirs();

        server.createContext("/upload", new UploadHandler());
        server.createContext("/download", new DownloadHandler());
        server.createContext("/", new CORSHandler());

        server.setExecutor(executorService);
    }

    public void start() {
        server.start();
        System.out.println("API server started on port " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        System.out.println("API server stopped");
    }

    private class CORSHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type,Authorization");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
            }

            String response = "NOT FOUND";
            exchange.sendResponseHeaders(404, response.getBytes().length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response.getBytes());
            }
        }
    }

    private class UploadHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String response = "Method not allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(response.getBytes());
                }
                return;
            }

            Headers requestHeaders = exchange.getRequestHeaders();
            String contentType = requestHeaders.getFirst("Content-Type");

            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                String response = "Bad Request: Content-Type must be multipart/form-data";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(response.getBytes());
                }
                return;
            }

            try {
                String boundary = contentType.substring(contentType.indexOf("boundary=") + 1);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                IOUtils.copy(exchange.getRequestBody(), byteArrayOutputStream);
                byte[] requestData = byteArrayOutputStream.toByteArray();

                Multiparser parser = new Multiparser(requestData, boundary);
                ParseResult result = parser.parse();

                if (result == null) {
                    String response = "Bad Request: Could not parse file content";
                    exchange.sendResponseHeaders(400, response.getBytes().length);
                    try (OutputStream outputStream = exchange.getResponseBody()) {
                        outputStream.write(response.getBytes());
                    }
                    return;
                }

                String filename = result.filename();
                if (filename == null || filename.trim().isEmpty()) {
                    filename = "unnamed-file";
                }

                String uploadFilename = UUID.randomUUID().toString() + "_" + new File(filename).getName();
                String filePath = uploadDir + File.separator + uploadFilename;

                try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
                    fileOutputStream.write(result.fileContent());
                }

                int port = fileSharer.offerFile(filePath);
                new Thread(() -> fileSharer.startFileServer(port)).start();
                String jsonResponse = "{\"port\": }" + port + "}";

                headers.add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(jsonResponse.getBytes());
                }

            } catch (Exception e) {
                System.err.println("Error processing file upload: " + e.getMessage());
                String response = "Server error: " + e.getMessage();
                exchange.sendResponseHeaders(500, response.getBytes().length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(response.getBytes());
                }
            }
        }
    }

    private class DownloadHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                String response = "Method not allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(response.getBytes());
                }
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String portStr = path.substring(path.lastIndexOf('/' + 1));

            try {
                int port = Integer.parseInt(portStr);

                try (Socket socket = new Socket("localhost", port)) {
                    InputStream socketInput = socket.getInputStream();
                    
                    File tempFile = File.createTempFile("download-", ".tmp");
                    String filename = "download-file";

                    try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;

                        ByteArrayOutputStream headerOutputStream = new ByteArrayOutputStream();
                        int b;
                        while ((b = socketInput.read()) != -1) {
                            if (b == '\n') break;
                            headerOutputStream.write(b);
                        }

                        String header = headerOutputStream.toString().trim();
                        if (header.startsWith("Filename: ")) {
                            filename = header.substring("Filename: ".length());
                        }

                        while ((bytesRead = socketInput.read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0, bytesRead);
                        }
                    }

                    headers.add("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                    headers.add("Content-Type", "application/octet-stream");

                    exchange.sendResponseHeaders(200, tempFile.length());
                    try (OutputStream os = exchange.getResponseBody();
                         FileInputStream fis = new FileInputStream(tempFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }

                    tempFile.delete();

                } catch (IOException e) {
                    System.err.println("Error downloading file from peer: " + e.getMessage());
                    String response = "Error downloading file: " + e.getMessage();
                    headers.add("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(500, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }

            } catch (NumberFormatException e) {
                String response = "Bad Request: Invalid port number";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }

}
