package rf.robson;


import rf.robson.robotframework.RobotLibrary;
import rf.robson.jsonrpc.JsonError;
import rf.robson.jsonrpc.Request;
import rf.robson.jsonrpc.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintStream;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

public class REPL {
    private REPL() {}

    public static void start(RobotLibrary robotLibrary) {
        Scanner scanner = new Scanner(System.in, UTF_8.name());
        PrintStream utf8;
        try {
            utf8 = new PrintStream(System.out, true, UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        String line;

        while(scanner.hasNextLine() && !(line = scanner.nextLine()).equals("quit")) {
            Request request;

            try {
                request = new Request(new JSONObject(line));
                Response response = robotLibrary.handle(request);
                utf8.println(new JSONObject(response));
            }
            catch (JSONException e) {
                Response response = new Response(0, JsonError.parseError(e.getMessage()));
                utf8.println(new JSONObject(response));
            }
        }
    }
}
