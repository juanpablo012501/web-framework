package co.edu.escuelaing.webframework;

@FunctionalInterface
public interface RouteHandler {

    // El desarrollador implementa este método con una lambda
    // Ejemplo: (req, resp) -> "Hello " + req.getValue("name")
    String handle(Request req, Response resp) throws Exception;
}