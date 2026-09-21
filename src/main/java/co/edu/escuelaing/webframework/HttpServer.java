package co.edu.escuelaing.webframework;

import java.io.*;
import java.net.*;

public class HttpServer {

    private static boolean running = false;

    public static void start(int port, Router router, StaticFileService staticFileService) throws IOException {
        running = true;

        System.out.println("Server starting on port " + port);
        System.out.println("Open: http://localhost:" + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleRequest(clientSocket, router, staticFileService);
                } catch (Exception e) {
                    System.err.println("Error handling request: " + e.getMessage());
                }
            }
        }

        System.out.println("Server stopped gracefully.");
    }

    public static void stop() {
        running = false;
    }

    private static void handleRequest(Socket clientSocket, Router router,
                                      StaticFileService staticFileService) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
        OutputStream out = clientSocket.getOutputStream();

        // Leer línea de petición
        String requestLine = in.readLine();
        if (requestLine == null || requestLine.isBlank()) {
            sendError(out, 400, "Bad Request");
            return;
        }

        System.out.println(">>> " + requestLine);

        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            sendError(out, 400, "Bad Request");
            return;
        }

        String method = parts[0];
        String fullPath = parts[1];

        if (!method.equals("GET")) {
            sendError(out, 405, "Method Not Allowed");
            return;
        }

        // Separar path y query string
        String path;
        String queryString = "";
        int queryIndex = fullPath.indexOf('?');
        if (queryIndex >= 0) {
            path = fullPath.substring(0, queryIndex);
            queryString = fullPath.substring(queryIndex + 1);
        } else {
            path = fullPath;
        }

        if (path.equals("/")) {
            path = "/index.html";
        }

        // Crear objetos Request y Response
        Request req = new Request(method, path, queryString);
        Response resp = new Response();

        // 1. Buscar ruta dinámica en el Router
        RouteHandler handler = router.resolve(path);
        if (handler != null) {
            try {
                String result = handler.handle(req, resp);
                sendResponse(out, 200, resp.getContentType(), result.getBytes("UTF-8"));
            } catch (Exception e) {
                sendError(out, 500, "Internal Server Error: " + e.getMessage());
            }
            return;
        }

        // 2. Intentar servir archivo estático
        if (staticFileService.exists(path)) {
            byte[] fileBytes = staticFileService.getFile(path);
            String contentType = staticFileService.getContentType(path);
            sendResponse(out, 200, contentType, fileBytes);
            return;
        }

        // 3. Nada encontrado → 404
        sendError(out, 404, "Not Found: " + path);
    }

    private static void sendResponse(OutputStream out, int status,
                                     String contentType, byte[] body) throws IOException {
        String headers = "HTTP/1.1 " + status + " OK\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + body.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(headers.getBytes());
        out.write(body);
        out.flush();
    }

    private static void sendError(OutputStream out, int code, String message) throws IOException {
        byte[] body = (code + " " + message).getBytes("UTF-8");
        String headers = "HTTP/1.1 " + code + " " + message + "\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n" +
                "Content-Length: " + body.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(headers.getBytes());
        out.write(body);
        out.flush();
    }
}