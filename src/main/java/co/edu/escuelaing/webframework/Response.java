package co.edu.escuelaing.webframework;

public class Response {

    private int statusCode;
    private String contentType;
    private String body;

    public Response() {
        this.statusCode = 200;
        this.contentType = "text/plain; charset=UTF-8";
        this.body = "";
    }

    public void setStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getContentType() {
        return contentType;
    }

    public String getBody() {
        return body;
    }
}