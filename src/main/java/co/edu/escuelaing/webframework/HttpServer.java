package co.edu.escuelaing.webframework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servidor HTTP con manejo concurrente de peticiones y apagado ordenado.
 *
 * - Concurrencia: el hilo principal solo acepta conexiones; cada conexion se
 *   atiende en un hilo de un pool de tamano fijo.
 * - Graceful shutdown: al recibir stop() o una senal SIGTERM/SIGINT deja de
 *   aceptar conexiones nuevas, espera a que terminen las peticiones en curso
 *   (hasta un tiempo maximo) y solo entonces termina.
 */
public class HttpServer {

    private static final int SOCKET_TIMEOUT_MS = 10_000;

    private final int port;
    private final Router router;
    private final StaticFileService staticFileService;
    private final int poolSize;
    private final long shutdownTimeoutSeconds;

    private volatile boolean running = false;
    private volatile ServerSocket serverSocket;
    private ExecutorService pool;

    private final CountDownLatch ready = new CountDownLatch(1);
    private final CountDownLatch stopped = new CountDownLatch(1);
    private final AtomicInteger threadCounter = new AtomicInteger();

    public HttpServer(int port, Router router, StaticFileService staticFileService,
                      int poolSize, long shutdownTimeoutSeconds) {
        this.port = port;
        this.router = router;
        this.staticFileService = staticFileService;
        this.poolSize = poolSize;
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
    }

    /** Bloquea el hilo que lo llama hasta que el servidor se detiene. */
    public void start() throws IOException {
        pool = Executors.newFixedThreadPool(poolSize, task ->
                new Thread(task, "http-worker-" + threadCounter.incrementAndGet()));

        Thread shutdownHook = new Thread(() -> {
            if (running) {
                System.out.println("Shutdown signal received.");
                stop();
                awaitStopped(shutdownTimeoutSeconds + 5);
            }
        }, "shutdown-hook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Server starting on port " + getLocalPort()
                    + " with " + poolSize + " worker threads");
            System.out.println("Open: http://localhost:" + getLocalPort());
            ready.countDown();

            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    try {
                        pool.execute(() -> handleConnection(client));
                    } catch (RejectedExecutionException e) {
                        closeQuietly(client);
                    }
                } catch (SocketException e) {
                    if (running) {
                        System.err.println("Accept error: " + e.getMessage());
                    }
                    // Si running es false, stop() cerro el socket: salimos del ciclo.
                }
            }
        } finally {
            running = false;
            ready.countDown();
            drain();
            stopped.countDown();
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
                // La JVM ya esta apagandose: el hook se esta ejecutando.
            }
        }
    }

    /**
     * Solicita el apagado. No bloquea: solo deja de aceptar conexiones nuevas.
     * Es seguro llamarlo desde un handler (por ejemplo /shutdown), porque la
     * espera de las peticiones en curso la hace el hilo que ejecuta start().
     */
    public void stop() {
        running = false;
        ServerSocket s = serverSocket;
        if (s != null) {
            try {
                s.close(); // desbloquea accept()
            } catch (IOException ignored) {
            }
        }
    }

    public boolean awaitReady(long seconds) throws InterruptedException {
        return ready.await(seconds, TimeUnit.SECONDS);
    }

    public boolean awaitStopped(long seconds) {
        try {
            return stopped.await(seconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public int getLocalPort() {
        ServerSocket s = serverSocket;
        return s == null ? port : s.getLocalPort();
    }

    private void drain() {
        System.out.println("Not accepting new connections. Waiting for in-flight requests...");
        pool.shutdown();
        boolean forced = false;
        try {
            if (!pool.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                System.err.println("Timeout after " + shutdownTimeoutSeconds
                        + "s: forcing shutdown of remaining requests.");
                pool.shutdownNow();
                forced = true;
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            forced = true;
            Thread.currentThread().interrupt();
        }
        System.out.println(forced ? "Server stopped (some requests were interrupted)."
                                  : "Server stopped gracefully.");
    }

    private void handleConnection(Socket clientSocket) {
        try (Socket socket = clientSocket) {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            handleRequest(socket);
        } catch (Exception e) {
            System.err.println("Error handling request: " + e.getMessage());
        }
    }

    private void handleRequest(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
        OutputStream out = clientSocket.getOutputStream();

        // Leer linea de peticion
        String requestLine = in.readLine();
        if (requestLine == null) {
            return; // el cliente cerro la conexion sin enviar nada
        }
        if (requestLine.isBlank()) {
            sendError(out, 400, "Bad Request");
            return;
        }

        // Consumir los headers restantes hasta la linea vacia
        String headerLine;
        while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
            // los headers no se usan por ahora
        }

        System.out.println(">>> [" + Thread.currentThread().getName() + "] " + requestLine);

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

        // 1. Buscar ruta dinamica en el Router
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

        // 2. Intentar servir archivo estatico
        if (staticFileService.exists(path)) {
            byte[] fileBytes = staticFileService.getFile(path);
            String contentType = staticFileService.getContentType(path);
            sendResponse(out, 200, contentType, fileBytes);
            return;
        }

        // 3. Nada encontrado -> 404
        sendError(out, 404, "Not Found: " + path);
    }

    private void sendResponse(OutputStream out, int status,
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

    private void sendError(OutputStream out, int code, String message) throws IOException {
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

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
