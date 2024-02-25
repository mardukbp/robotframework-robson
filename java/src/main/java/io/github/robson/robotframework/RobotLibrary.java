package io.github.robson.robotframework;


import io.github.robson.jsonrpc.JsonError;
import io.github.robson.jsonrpc.Request;
import io.github.robson.jsonrpc.IRequestHandler;
import io.github.robson.jsonrpc.Response;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * <a href="https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#dynamic-library-api">
 * Dynamic Library API
 * </a>
 */
public class RobotLibrary implements IRequestHandler {
    private final Map<String, String> pythonTypes = new HashMap<String, String>(){{
        put("int", "int");
        put("boolean", "bool");
        put("double", "float");
        put("String", "str");
    }};

    private boolean isValidKeyword(Method method) {
        return method.isAnnotationPresent(Keyword.class) && method.isAnnotationPresent(Doc.class);
    }

    private final Supplier<String> introDoc = () -> {
        if (this.getClass().isAnnotationPresent(Doc.class))
            return this.getClass().getAnnotation(Doc.class).doc();
        else
            return "The library has no documentation";
    };

    private final Map<String, RobotKeyword> keywords =
            Arrays.stream(this.getClass().getDeclaredMethods())
                    .filter(this::isValidKeyword)
                    .collect(Collectors.toMap(
                            (method -> method.getAnnotation(Keyword.class).name()),
                            (method -> new RobotKeyword(
                                    method,
                                    method.getAnnotation(Keyword.class).name(),
                                    method.getAnnotation(Doc.class).doc(),
                                    Arrays.stream(method.getParameters()).map(Parameter::getName).collect(Collectors.toList()),
                                    Arrays.stream(method.getParameterTypes()).map(type -> pythonTypes.get(type.getSimpleName())).collect(Collectors.toList())
                            ))
            ));

    public String[] getKeywordArguments(String keyword) {
        return keywords.get(keyword).args.toArray(new String[0]);
    }

    public String getKeywordDocumentation(String keyword) {
        switch (keyword) {
            case "__intro__":
                return introDoc.get();
            case "__init__":
                return "Library has no init documentation.";
            default:
                return keywords.get(keyword).doc;
        }
    }

    public String[] getKeywordNames() {
        return keywords.keySet().toArray(new String[0]);
    }

    public String[] getKeywordTypes(String keyword) {
        return keywords.get(keyword).types.toArray(new String[0]);
    }

    public Object runKeyword(String keyword, Object[] args) throws InvocationTargetException, IllegalAccessException {
        return keywords.get(keyword).method.invoke(this, args);
    }

    @Override
    public Response handle(Request request) {
        switch (request.method) {
            case "get_keyword_arguments": {
                String keyword = request.params[0].toString();
                return new Response(request.id, getKeywordArguments(keyword));
            }

            case "get_keyword_documentation": {
                String keyword = request.params[0].toString();
                return new Response(request.id, getKeywordDocumentation(keyword));
            }

            case "get_keyword_names":
                return new Response(request.id, getKeywordNames());

            case "get_keyword_types": {
                String keyword = request.params[0].toString();
                return new Response(request.id, getKeywordTypes(keyword));
            }

            case "run_keyword": {
                String keyword = request.params[0].toString();
                Object[] args = Arrays.stream(request.params).skip(1).toArray();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                System.setOut(new PrintStream(stream));

                try {
                    Object returnValue = runKeyword(keyword, args);
                    String output = stream.toString();
                    RobotResult robotResult = RobotResult.pass(
                            output,
                            returnValue
                    );
                    return new Response(request.id, robotResult);
                }
                catch (Exception e) {
                    Throwable throwable;

                    if (e.getCause() != null) {
                        throwable = e.getCause();
                    }
                    else {
                        throwable = e;
                    }

                    StringWriter sw = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sw));
                    String traceback = sw.toString();
                    RobotResult robotResult = RobotResult.fail(
                            throwable.getMessage(),
                            traceback
                    );
                    return new Response(request.id, robotResult);
                }
            }

            default:
                return new Response(
                        request.id,
                        JsonError.applicationError("Unknown method", request.method)
                );
        }
    }
}