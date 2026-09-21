package co.edu.escuelaing.webframework;

import java.io.InputStream;

public class StaticFileService {

    private String staticRoot;

    public StaticFileService(String staticRoot) {
        this.staticRoot = staticRoot;
    }

    public void setStaticRoot(String staticRoot) {
        this.staticRoot = staticRoot;
    }

    // Retorna los bytes del archivo o null si no existe
    public byte[] getFile(String path) {
        // Seguridad: rechazar path traversal
        if (path.contains("..")) return null;

        String resourcePath = staticRoot + path;
        InputStream fileStream = StaticFileService.class.getResourceAsStream(resourcePath);

        if (fileStream == null) return null;

        try {
            return fileStream.readAllBytes();
        } catch (Exception e) {
            return null;
        }
    }

    // Verifica si el archivo existe
    public boolean exists(String path) {
        if (path.contains("..")) return false;
        String resourcePath = staticRoot + path;
        return StaticFileService.class.getResourceAsStream(resourcePath) != null;
    }

    public String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css"))  return "text/css; charset=UTF-8";
        if (path.endsWith(".js"))   return "application/javascript; charset=UTF-8";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".ico"))  return "image/x-icon";
        if (path.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }
}