package server;

public class Response {
    private final String response;
    private String value;
    private String reason;

    public Response(final String response) {
        this.response = response;
    }

    public Response(final String response, final String second) {
        this.response = response;
        if (response.equals("OK")) {
            this.value = second;
        } else {
            this.reason = second;
        }
    }
}
