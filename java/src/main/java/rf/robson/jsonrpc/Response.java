package rf.robson.jsonrpc;

public class Response {
    private final String jsonrpc;
    private final int id;
    private final JsonError error;
    private final Object result;

    public String getJsonrpc() {
        return jsonrpc;
    }

    public int getId() {
        return id;
    }

    public JsonError getError() {
        return error;
    }

    public Object getResult() {
        return result;
    }

    public Response(int id, Object result) {
        this.id = id;
        this.jsonrpc = "2.0";
        this.error = null;
        this.result = result;
    }

    public Response(int id, JsonError error) {
        this.id = id;
        this.jsonrpc = "2.0";
        this.error = error;
        this.result = null;
    }
}
