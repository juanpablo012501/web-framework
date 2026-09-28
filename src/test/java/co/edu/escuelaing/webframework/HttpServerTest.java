package co.edu.escuelaing.webframework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

public class HttpServerTest {

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpServer server;
    private Thread serverThread;

    private int startServer(int workers, int shutdownTimeoutSeconds) throws Exception {
        Router router = new Router();
        router.get("/ping", (req, resp) -> "pong");
        router.get("/slow", (req, resp) -> {
            Thread.sleep(600);
            return "done";
        });

        server = new HttpServer(0, router, new StaticFileService("/webroot"),
                workers, shutdownTimeoutSeconds);
        serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        serverThread.start();
        assertTrue(server.awaitReady(5));
        return server.getLocalPort();
    }

    private HttpResponse<String> get(int port, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + port + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void parallelRequestsAreNotSerialized() throws Exception {
        int port = startServer(8, 5);
        try {
            long start = System.nanoTime();
            List<CompletableFuture<HttpResponse<String>>> calls = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + "/slow")).GET().build();
                calls.add(client.sendAsync(request, HttpResponse.BodyHandlers.ofString()));
            }
            for (CompletableFuture<HttpResponse<String>> call : calls) {
                assertEquals(200, call.get().statusCode());
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            // Secuencial serian ~4800 ms (8 x 600 ms). En paralelo, cerca de 600 ms.
            assertTrue(elapsedMs < 3000, "took " + elapsedMs + " ms, requests look serialized");
        } finally {
            server.stop();
            server.awaitStopped(10);
        }
    }

    @Test
    public void idleConnectionDoesNotBlockOtherRequests() throws Exception {
        int port = startServer(4, 5);
        try (Socket idle = new Socket("127.0.0.1", port)) {
            HttpResponse<String> response = get(port, "/ping");
            assertEquals(200, response.statusCode());
            assertEquals("pong", response.body());
        } finally {
            server.stop();
            server.awaitStopped(10);
        }
    }

    @Test
    public void gracefulShutdownLetsInFlightRequestFinish() throws Exception {
        int port = startServer(4, 5);

        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + port + "/slow")).GET().build();
        CompletableFuture<HttpResponse<String>> inFlight =
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        Thread.sleep(200); // la peticion ya esta ejecutandose
        server.stop();

        HttpResponse<String> response = inFlight.get();
        assertEquals(200, response.statusCode());
        assertEquals("done", response.body());

        assertTrue(server.awaitStopped(10), "server did not stop after draining");

        boolean refused = false;
        try (Socket late = new Socket()) {
            late.connect(new InetSocketAddress("127.0.0.1", port), 1000);
        } catch (ConnectException e) {
            refused = true;
        }
        assertTrue(refused, "server should refuse new connections after stop()");
    }
}
