package co.edu.escuelaing.webframework;

public class WebFramework {

    private static final Router router = new Router();
    private static volatile StaticFileService staticFileService = new StaticFileService("/webroot");
    private static volatile HttpServer server;

    // Registra una ruta GET con una lambda
    // Ejemplo: get("/hello", (req, resp) -> "Hello " + req.getValue("name"));
    public static void get(String path, RouteHandler handler) {
        router.get(path, handler);
    }

    // Configura la carpeta de recursos estaticos
    // Ejemplo: staticfiles("/webroot");
    public static void staticfiles(String path) {
        staticFileService = new StaticFileService(path);
    }

    // Inicia el servidor. Toda la configuracion viene de variables de entorno:
    //   PORT                      puerto de escucha (default 8080)
    //   WORKER_THREADS            hilos que atienden peticiones (default 32)
    //   SHUTDOWN_TIMEOUT_SECONDS  espera maxima por peticiones en curso (default 8)
    public static void start() throws Exception {
        int port = envInt("PORT", 8080);
        int workers = envInt("WORKER_THREADS", 32);
        int shutdownTimeout = envInt("SHUTDOWN_TIMEOUT_SECONDS", 8);

        server = new HttpServer(port, router, staticFileService, workers, shutdownTimeout);
        server.start();
    }

    // Solicita el apagado ordenado del servidor (no bloquea)
    public static void stop() {
        HttpServer current = server;
        if (current != null) {
            current.stop();
        }
    }

    private static int envInt(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid value for " + name + ": '" + value
                    + "'. Using default " + defaultValue);
            return defaultValue;
        }
    }
}
