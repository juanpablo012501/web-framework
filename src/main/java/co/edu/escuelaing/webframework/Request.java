package co.edu.escuelaing.webframework;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class Request {

    private final String method;
    private final String path;
    private final String queryString;
    private final Map<String, String> params;

    public Request(String method, String path, String queryString) {
        this.method = method;
        this.path = path;
        this.queryString = queryString;
        this.params = parseQueryString(queryString);
    }

    // Obtener el valor de un parámetro del query string
    // Ejemplo: /hello?name=Pedro → getValue("name") → "Pedro"
    public String getValue(String key) {
        return params.get(key);
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getQueryString() {
        return queryString;
    }

    private Map<String, String> parseQueryString(String queryString) {
        Map<String, String> result = new HashMap<>();
        if (queryString == null || queryString.isBlank()) return result;

        for (String pair : queryString.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try {
                    String key = URLDecoder.decode(kv[0], "UTF-8");
                    String value = URLDecoder.decode(kv[1], "UTF-8");
                    result.put(key, value);
                } catch (Exception e) {
                    // par inválido, se ignora
                }
            }
        }
        return result;
    }
}