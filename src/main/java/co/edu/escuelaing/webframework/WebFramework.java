package co.edu.escuelaing.webframework;

public class WebFramework {

    private static final Router router = new Router();
    private static StaticFileService staticFileService = new StaticFileService("/webroot");

    // Registra una ruta GET con una lambda
    // Ejemplo: get("/hello", (req, resp) -> "Hello " + req.getValue("name"));
    public static void get(String path, RouteHandler handler) {
        router.get(path, handler);
    }

    // Configura la carpeta de recursos estáticos
    // Ejemplo: staticfiles("/webroot");
    public static void staticfiles(String path) {
        staticFileService = new StaticFileService(path);
    }

    // Inicia el servidor leyendo el puerto de la variable de entorno PORT
    public static void start() throws Exception {
        String portEnv = System.getenv("PORT");
        int port = (portEnv == null || portEnv.isBlank()) ? 8080 : Integer.parseInt(portEnv);
        HttpServer.start(port, router, staticFileService);
    }

    // Detiene el servidor gracefully
    public static void stop() {
        HttpServer.stop();
    }
}