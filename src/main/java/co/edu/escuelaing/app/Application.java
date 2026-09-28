package co.edu.escuelaing.app;

import static co.edu.escuelaing.webframework.WebFramework.*;

public class Application {

    public static void main(String[] args) throws Exception {

        staticfiles("/webroot");

        // Ruta: saludo con nombre
        get("/hello", (req, resp) -> {
            String name = req.getValue("name");
            if (name == null || name.isBlank()) {
                name = "world";
            }
            String prefix = System.getenv().getOrDefault("GREETING_PREFIX", "Hello");
            return prefix + ", " + name + "!";
        });

        // Ruta: valor de PI
        get("/pi", (req, resp) ->
                String.valueOf(Math.PI));

        // Ruta: cuadrado de un número
        get("/square", (req, resp) -> {
            String value = req.getValue("value");
            if (value == null || value.isBlank()) {
                return "Missing parameter: value";
            }
            try {
                double num = Double.parseDouble(value);
                return "{\"input\": " + num + ", \"square\": " + (num * num) + "}";
            } catch (NumberFormatException e) {
                return "Invalid number: " + value;
            }
        });

        // Ruta: hora del servidor
        get("/time", (req, resp) -> {
            String now = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return "{\"serverTime\": \"" + now + "\"}";
        });

        // Ruta de demostracion de concurrencia: responde tras esperar ms milisegundos
        // (maximo 5000) e indica que hilo del pool atendio la peticion.
        get("/sleep", (req, resp) -> {
            long ms = 1000;
            try {
                ms = Long.parseLong(req.getValue("ms"));
            } catch (Exception e) {
                // parametro ausente o invalido: se usa el valor por defecto
            }
            ms = Math.max(0, Math.min(ms, 5000));
            Thread.sleep(ms);
            return "Slept " + ms + " ms on " + Thread.currentThread().getName();
        });

        // Shutdown solo en desarrollo
        String env = System.getenv().getOrDefault("APP_ENV", "development");
        if (env.equals("development")) {
            get("/shutdown", (req, resp) -> {
                stop();
                return "Server will stop after this response.";
            });
        }

        start();
    }
}