/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.rest;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import gaiasky.GaiaSky;
import gaiasky.script.IScriptingInterface;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import spark.Spark;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

import static spark.Spark.*;

/*
 * REST API for remote procedure calls
 *
 * @author Volker Gaibler, HITS
 * <p>
 * WARNING: only allow and use this in a trusted environment. Incoming commands are
 * *not checked at all* before execution! Remote command execution is generally
 * dangerous. This REST API was developed for an exhibition with an isolated network.
 * <p>
 * The API allows calling methods from the scripting interface
 * ({@link IScriptingInterface}) remotely via HTTP for remote control.
 * <p>
 * Syntax of API commands is set to be close to the Java method interface, but does not cover
 * it in all generality to permit simple usage. Particularly note that the REST server receives
 * strings from the client and will try to convert them into correct types.
 * <p>
 * Commands require HTTP request parameter having the names for the formal parameters of the
 * script interface methods to allow simple construction of HTTP requests based on the scripting
 * interface source documentation. We use Java reflections with access to the formal parameter
 * names. Accordingly, the code needs to be compiled with "-parameters" (otherwise parameters
 * are named arg0, arg1, ...).
 * <p>
 * Both GET and POST requests are accepted. Although GET requests are not supposed to have
 * side effects, we include them for easy usage with a browser.
 * <p>
 * Issue commands with a syntax like the following:
 * - http://localhost:8080/api/setCameraUp?up=[1.,0.,0.]
 * - http://localhost:8080/api/getScreenWidth
 * - http://localhost:8080/api/goToObject?name=Jupiter&angle=32.9&focusWait=2
 * <p>
 * Give booleans, ints, floats, doubles, strings as they are, vectors comma-separated with
 * square brackets around: true, 42, 3.1, 3.14, Super-string, [1,2,3], [Do,what,they,told,ya].
 * Note that you might need to escape or url-encode characters in a browser for this
 * (e.g. spaces or "=").
 * <p>
 * Response with return data is in JSON format, containing key/value pairs.
 * The "success" pair tells you about success/failure of the call,
 * the "value" pair gives the return value. Void methods will contain a "null" return value.
 * The "text" pair can give additional information on the call.
 * <p>
 * The 'cmd_syntax' entry you get from the 'help' command (e.g. http://localhost:8080/api/help)
 * gives a summary of permitted commands and their return type. Details on the meaning of the
 * command and its parameters need to be found from the scripting API documentation:
 * https://gaia.ari.uni-heidelberg.de/gaiasky/docs/javadoc/latest/gaiasky/script/IScriptingInterface.html
 * <p>
 * To examine, what happens during an API call, set the default log level of SimpleLogger to
 * 'info' or lower (in core/build.gradle).
 * <p>
 * Return values are given as JSON objects that contain key-value pairs:
 * - "success" indicates whether the API call was executed successful or not
 * - "text" may give additional text information
 * - "value" contains the return value or null if there is no return value
 * <p>
 * For testing with curl, a call like the following allows will deal with url-encoding:
 * curl "http://localhost:8080/api/setHeadlineMessage" --data headline='Hi, how are you?'
 */

/**
 * REST Server class to implement the REST API
 *
 * @author Volker Gaibler
 * <p>
 * Implemented with Spark, which launches an embedded jetty. Spark
 * recommends static context.
 * <p>
 * This gets initialized in
 * core/src/gaiasky/desktop/GaiaSkyDesktop.java with some
 * lazy initialization since Spark wants to be used in static context.
 */
public class RESTServer {

    /* Class variables: */

    /**
     * "Shutdown already triggered" flag. {@link Spark#stop()} can be called multiple times
     * (multiple events), but only processed once.
     */
    private static boolean shutdownTriggered = false;

    /**
     * Activated flag. Calling API methods generally requires the GUI to be fully
     * started and all objects initialized, indicated by the "activated" flag. This
     * flag is set true through the {@link RESTServer#activate()} method that needs to be called
     * externally once the GUI is ready.
     */
    private static boolean activated = false;

    /**
     * REST server port. TCP port the server is listening on. It is set on
     * initialization.
     */
    private static Integer port = -1;

    /**
     * REST server static files location.
     */
    private static final String rest_static_location = Settings.ASSETS_LOC + "/rest-static";

    /**
     * Logger
     */
    private static final Log logger = Logger.getLogger(RESTServer.class);

    /**
     * Name to method map
     */
    private static Map<String, Array<Method>> methodMap;

    /* Methods: */

    /**
     * Prints startup warning and current log level of SimpleLogger.
     */
    private static void printStartupInfo() {
        String s = System.getProperty("org.slf4j.simpleLogger.defaultLogLevel");
        logger.debug("Simple Logger defaultLogLevel = " + s);
        logger.warn("*** Warning: REST API server may permit remote code execution! " + "Only use this functionality in a trusted environment! ***");
    }

    /**
     * Sets the HTTP response in JSON format. - Return values are returned under the
     * "value" key. - Additional keys may provide further data or information. - The
     * "text" key is encouraged for human-readable information.
     */
    private static String responseData(spark.Request request, spark.Response response, Map<String, Object> ret, boolean success) {

        String responseString;

        /* Content-Type */
        response.type("application/json");

        /* return value is null for void methods */
        ret.putIfAbsent("value", null);

        /* success key and HTTP status code */
        ret.put("success", success);
        if (success) {
            response.status(200); // 200 OK
            ret.putIfAbsent("text", "OK");
        } else {
            response.status(400); // 400 Bad Request
            ret.putIfAbsent("text", "Failed");
        }

        /* header */
        // response.header("FOO", "bar");

        /* request body */
        Json json = new Json(OutputType.json);
        responseString = json.toJson(ret);
        logger.debug("HTTP response body: {}.", responseString);
        return responseString;
    }

    /**
     * Log information on the request.
     */
    private static void loggerRequestInfo(spark.Request request) {
        logger.debug("======== Handling API call via HTTP {}: ========", request.requestMethod());
        logger.debug("* Parameter extracted:");
        logger.debug("  command = ", request.params(":cmd"));
        logger.debug("* Request:");
        logger.debug("  client IP = {}", request.ip());
        logger.debug("  host = {}", request.host());
        logger.debug("  userAgent = {}", request.userAgent());
        logger.debug("  pathInfo = {}", request.pathInfo());
        logger.debug("  servletPath = {}", request.servletPath());
        logger.debug("  contextPath = {}", request.contextPath());
        logger.debug("  url = {}", request.url());
        logger.debug("  uri = {}", request.uri());
        logger.debug("  protocol = {}", request.protocol());
        logger.debug("* Body");
        logger.debug("  contentType() = '{}'", request.contentType());
        logger.debug("  params() = '{}'", request.params());
        logger.debug("  body contentLenght() = {}", request.contentLength());
        // NOTE: when calling method body(), the body is consumed and queryParams()
        // doesn't find
        // the parameters anymore!
        // logger.debug("body() = '{}'", request.body());
        logger.debug("* Query parameters");
        logger.debug("  queryString() = '{}'", request.queryString());
        logger.debug("  queryParams = {}", request.queryParams());
        for (String s : request.queryParams()) {
            logger.debug("    '{}' => '{}'", s, request.queryParams(s));
        }
    }

    /**
     * Returns a declaration string for the given method.
     */
    private static String methodDeclarationString(Method method) {
        Parameter[] methodParams = method.getParameters();

        StringBuilder ret = new StringBuilder(method.getName());
        for (int i = 0; i < methodParams.length; i++) {
            Parameter p = methodParams[i];
            ret.append(String.format("%s%s=(%s)", ((i == 0) ? "?" : "&"), p.getName(), p.getType().getSimpleName()));
        }
        // \u27F6 is "⟶"
        ret.append(String.format(" \u27F6 %s", method.getReturnType().getSimpleName()));
        return ret.toString();
    }

    /**
     * Returns a list of all matching method declaration strings. To get a list of
     * all method declarations, use empty string for <code>methodName</code>.
     */
    private static String[] getMethodDeclarationStrings(String methodName) {
        Method[] allMethods = IScriptingInterface.class.getDeclaredMethods();

        List<String> matchMethodsDeclarations = new ArrayList<>();
        for (Method allMethod : allMethods) {
            if (methodName.length() == 0 || methodName.equals(allMethod.getName())) {
                String declaration = methodDeclarationString(allMethod);
                matchMethodsDeclarations.add(declaration);
            }
        }
        Collections.sort(matchMethodsDeclarations);
        return matchMethodsDeclarations.toArray(new String[0]);
    }

    /**
     * Converts an array-representing string and returns it as array of strings.
     * This defines how array need to be passed as HTTP request parameters:
     * comma-separated and enclosed in square brackets, e.g. "[var1,var2,var3]"
     */
    private static String[] splitArrayString(String arrayString) {
        int len = arrayString.length();
        if (len >= 2 && "[".equals(arrayString.substring(0, 1)) && "]".equals(arrayString.substring(len - 1, len))) {
            return arrayString.substring(1, len - 1).split(",");
        } else {
            // probably an array should never be empty
            logger.warn("splitArrayString: '{}' is parsed as empty array!", arrayString);
            throw new IllegalArgumentException();
            // emtpy array
            // return new String[0];
        }
    }

    /**
     * Handles the API call.
     * <p>
     * This is implemented via Java Reflections and gives access to all methods from
     * IScriptingInterface
     * (core/src/gaiasky/script/IScriptingInterface.java).
     * Additionally, it provides "special-purpose commands", see commented source
     * block.
     * <p>
     * Since HTTP request variables are all strings and do provide neither argument
     * indices nor argument types, there cannot be a direct translation to Java
     * without adding additional information (which would make the HTTP request
     * harder to read and write. If this ever becomes an issue, we could get
     * optionally add a type to the parameters, e.g. "distance_float=0.3f" and split
     * by the underscore.
     */
    private static String handleApiCall(spark.Request request, spark.Response response) {

        // Logging basic request information
        loggerRequestInfo(request);

        // map containing information for http return response
        Map<String, Object> ret = new HashMap<>();

        // Only process API calls if already activated (GUI launched).
        if (!activated) {
            String msg = "GUI not yet initialized. Please wait...";
            logger.warn(msg);
            ret.put("text", msg);
            return responseData(request, response, ret, false);
        }

        // Extract command from http request
        String cmd = request.params(":cmd");

        // params
        Set<String> queryParams = request.queryParams();

        /* Special-treatment commands */
        if ("help".equals(cmd)) {
            logger.debug("Help command received");
            ret.put("text", "Help: see 'cmd_syntax' for command reference. " + "Vectors are comma-separated.");
            ret.put("cmd_syntax", getMethodDeclarationStrings(""));
            return responseData(request, response, ret, true);
        } else if ("debugCall".equals(cmd)) {
            logger.debug("debugCall received. What to do now?");
            ret.put("text", "debugCall data");
            return responseData(request, response, ret, true);
        }

        /* Method matching (name and parameters) */
        logger.debug("Method matching...");
        Method matchMethod = null;
        boolean methodNameMatches = false;

        if (!methodMap.containsKey(cmd)) {
            /* No match: could not find matching method */
            logger.debug("No suitable method found.");

            String msg = String.format("Failed: command name '%s' not found. " + "See syntax in 'cmd_syntax'.", cmd);
            ret.put("cmd_syntax", getMethodDeclarationStrings(""));
            logger.warn(msg);
            ret.put("text", msg);
            return responseData(request, response, ret, false);
        }

        Array<Method> matchMethods = methodMap.get(cmd);

        for (int i = 0; i < matchMethods.size; i++) {
            Method m = matchMethods.get(i);
            logger.debug("match check cmd={} with method={}...", cmd, m.getName());

            // name matches, but parameters may be different
            if (m.getName().equals(cmd)) {
                logger.debug("  [+] name matches");
                methodNameMatches = true;
                Parameter[] methodParams = matchMethods.get(i).getParameters();

                // check if parameters present (and optionally type fits?)
                boolean allParamsFound = true;
                int pi = 0;
                for (Parameter p : methodParams) {
                    if (!queryParams.contains(p.getName()) && !queryParams.contains("arg" + pi)) {
                        allParamsFound = false;
                        logger.debug("  [+] method parameters not present");
                        break; // no need to continue checking
                    }
                    pi++;
                    // could test for parameter type here...
                }

                if (allParamsFound) {
                    logger.debug("  [+] method parameters ok");
                    matchMethod = m;
                    break; // no need to continue checking: the first match
                }
            }
        }

        /* Handle matching result */
        if (matchMethod != null) {
            /* Found suitable method */
            logger.debug("Suitable method found: {}", methodDeclarationString(matchMethod));

            Parameter[] matchParameters = matchMethod.getParameters();
            Class<?> matchReturnType = matchMethod.getReturnType();

            Object[] arguments = new Object[matchParameters.length];
            Class<?>[] types = new Class[matchParameters.length];

            /* Prepare method arguments */
            logger.debug("Preparing method arguments...");
            for (int i = 0; i < matchParameters.length; i++) {
                Parameter p = matchParameters[i];
                String paramName = p.getName();
                String stringValue = request.queryParams(paramName);
                if (stringValue == null) {
                    // Try with arg+i
                    paramName = "arg" + i;
                    stringValue = request.queryParams(paramName);
                }
                logger.debug("  [+] handling parameter '{}'", paramName);

                // Set type
                types[i] = p.getType();
                logger.debug("  [+] parameter getType = '{}', isPrimitive = {}", p.getType().getSimpleName(), p.getType().isPrimitive());

                /*
                 * Set argument value, handling depends on type: We test against both primitives
                 * (.TYPE) and objects (.class). You might need to add more in the future if you
                 * trigger exceptions here...
                 */
                try {
                    if (Integer.TYPE.equals(types[i]) || Integer.class.equals(types[i])) {
                        logger.debug("  [+] handling parameter as type int");
                        arguments[i] = Integer.parseInt(stringValue);

                    } else if (Long.TYPE.equals(types[i]) || Long.class.equals(types[i])) {
                        logger.debug("  [+] handling parameter as type long");
                        arguments[i] = Long.parseLong(stringValue);

                    } else if (Float.TYPE.equals(types[i]) || Float.class.equals(types[i])) {
                        logger.debug("  [+] handling parameter as type float");
                        arguments[i] = Float.parseFloat(stringValue);

                    } else if (Double.TYPE.equals(types[i]) || Double.class.equals(types[i])) {
                        logger.debug("  [+] handling parameter as type double");
                        arguments[i] = Double.parseDouble(stringValue);

                    } else if (Boolean.TYPE.equals(types[i]) || Boolean.class.equals(types[i])) {
                        logger.debug("  [+] handling parameter as type boolean");
                        arguments[i] = Boolean.parseBoolean(stringValue);

                    } else if (int[].class.equals(types[i]) || Integer[].class.equals(types[i])) {
                        logger.debug("handling parameter as type int[]");
                        String[] svec = splitArrayString(stringValue);
                        int[] dvec = new int[svec.length];
                        for (int vi = 0; vi < svec.length; vi++) {
                            dvec[vi] = Integer.parseInt(svec[vi]);
                        }
                        arguments[i] = dvec;
                        logger.debug("  [+] argument={}", Arrays.toString(dvec));

                    } else if (float[].class.equals(types[i]) || Float[].class.equals(types[i])) {
                        logger.debug("  [+] handling parameter as type float[]");
                        String[] svec = splitArrayString(stringValue);
                        float[] dvec = new float[svec.length];
                        for (int vi = 0; vi < svec.length; vi++) {
                            dvec[vi] = Float.parseFloat(svec[vi]);
                        }
                        arguments[i] = dvec;
                        logger.debug("  [+] argument={}", Arrays.toString(dvec));

                    } else if (double[].class.equals(types[i]) || Double[].class.equals(types[i])) {
                        logger.debug("  [+] handling parameter as type double[]");
                        String[] svec = splitArrayString(stringValue);
                        double[] dvec = new double[svec.length];
                        for (int vi = 0; vi < svec.length; vi++) {
                            dvec[vi] = Double.parseDouble(svec[vi]);
                        }
                        arguments[i] = dvec;
                        logger.debug("  [+] argument={}", Arrays.toString(dvec));

                    } else if (String[].class.equals(types[i])) {
                        logger.debug("  [+] handling parameter as type String[]");
                        String[] svec = splitArrayString(stringValue);
                        arguments[i] = svec;
                        logger.debug("  [+] argument={}", Arrays.toString(svec));

                    } else {
                        logger.debug("  [+] handling parameter as type String");
                        // String also if it is some other, will raise exception
                        arguments[i] = stringValue;
                    }

                } catch (IllegalArgumentException e) {
                    String msg = String.format("Argument failure with parameter '%s'", p.getName());
                    logger.warn(msg);
                    ret.put("text", msg);
                    ret.put("cmd_syntax", getMethodDeclarationStrings(cmd));
                    return responseData(request, response, ret, false);
                }
            }

            /* Invoke method */
            try {
                logger.debug("Invoking method...");
                // note: invoke may return null explicitly or because is void type
                Object returnObject = matchMethod.invoke(GaiaSky.instance.scripting(), arguments);
                if (returnObject == null) {
                    logger.debug("Method returned: '{}', return type is {}", returnObject, matchReturnType);
                } else {
                    logger.debug("Method returned: '{}', isArray={}", returnObject, returnObject.getClass().isArray());
                }
                ret.put("value", returnObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return responseData(request, response, ret, true);

        } else {
            /* No match: could not find matching method */
            logger.debug("No suitable method found.");

            String msg;
            if (methodNameMatches) {
                msg = String.format("Failed: command name '%s' found, " + "but arguments not compatible.See syntax in 'cmd_syntax'.", cmd);
                ret.put("cmd_syntax", getMethodDeclarationStrings(cmd));
            } else {
                msg = String.format("Failed: command name '%s' not found. " + "See syntax in 'cmd_syntax'.", cmd);
                ret.put("cmd_syntax", getMethodDeclarationStrings(""));
            }
            logger.warn(msg);
            ret.put("text", msg);
            return responseData(request, response, ret, false);
        }
    }

    /**
     * Initialize the REST server.
     * <p>
     * Sets the routes and then passes the call to the handler.
     *
     * @param rest_port The port to use for the REST server
     */
    public static void initialize(Integer rest_port) {

        /* Check for valid TCP port (otherwise considered as "disabled") */
        port = rest_port;
        printStartupInfo();
        if (port < 0) {
            logger.error("Error: invalid port. REST API inactive.");
            return;
        }

        try {
            logger.info("Starting REST API server on http://localhost:{}/api/", port);
            logger.info("   See available calls at http://localhost:{}/api/help", port);
            port(port);
            logger.info("Setting routes");

            /*
             * Static file location:
             * add static HTML files with API use examples.
             * Note: this logs the value of spark.staticfiles.StaticFilesFolder
             * on warn level for information.
             */
            staticFiles.externalLocation(rest_static_location);

            /* Scripting API mapping */
            get("/api", (request, response) -> {
                response.redirect("/api/help");
                return response;
            });

            get("/api/:cmd", RESTServer::handleApiCall);

            post("/api/:cmd", RESTServer::handleApiCall);


            /* Initialize method index */
            // get set of permitted API commands
            Class<IScriptingInterface> iScriptingInterfaceClass = IScriptingInterface.class;
            Method[] allMethods = iScriptingInterfaceClass.getDeclaredMethods();

            methodMap = new HashMap<>();
            for (Method method : allMethods) {
                Array<Method> matches;
                if (methodMap.containsKey(method.getName())) {
                    matches = methodMap.get(method.getName());
                } else {
                    matches = new Array<>(false, 1);
                }
                if (!matches.contains(method, true))
                    matches.add(method);
                methodMap.put(method.getName(), matches);
            }

            logger.info("Startup finished.");

        } catch (Exception e) {
            logger.error(e, "Caught an exception during initialization:");
        }
    }

    /**
     * Activate. Set the "activated" flag for the server.
     */
    public static void activate() {
        activated = true;
    }

    /**
     * Stops the REST server gracefully.
     */
    public static void dispose() {
        try {
            if (!shutdownTriggered) {
                shutdownTriggered = true;
                logger.info("Stopping server gracefully...");
                stop();
                logger.info("Server now stopped.");
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

    }

}
