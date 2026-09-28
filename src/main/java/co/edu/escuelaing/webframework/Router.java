package co.edu.escuelaing.webframework;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Router {

    // Mapa de path → lambda handler
    // Ejemplo: "/hello" → (req, resp) -> "Hello world"
    private final Map<String, RouteHandler> routes = new ConcurrentHashMap<>();

    // Registra una ruta GET con su handler
    public void get(String path, RouteHandler handler) {
        routes.put(path, handler);
    }

    // Busca el handler para un path dado
    // Retorna null si no existe la ruta
    public RouteHandler resolve(String path) {
        return routes.get(path);
    }

    // Verifica si existe una ruta registrada para el path
    public boolean hasRoute(String path) {
        return routes.containsKey(path);
    }
}