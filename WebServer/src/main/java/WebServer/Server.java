package WebServer;

import WebServer.FHEMParser.FHEMParser;
import com.google.gson.Gson;

import io.vertx.core.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jdbc.JDBCAuth;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @date 16.06.17.
 */

// TODO: routeing mit Future in methode auslagern
// TODO: harcoded perm strings in fields auslagern


public class Server extends AbstractVerticle {
    private JDBCAuth authProvider;
    private Router router;
    private JDBCClient jdbcClient;
    private AuthHandler authHandler;
    private HttpServer server;
    private JsonObject jdbcClientConfig;
    private SQLConnection connection;
    private FHEMParser parser = Main.parser;

    private final static String OK_SERVER_RESPONSE = "OK";
    private final static String Registered_SERVER_RESPONSE = "Registered";
    private final static String ChangedRoomplan_SERVER_RESPONSE = "Changed Roomplan";
    private final static String ChangedSensorPosition_SERVER_RESPONSE = "Changed Sensor Position";
    private final static String BadRequest_SERVER_RESPONSE = "Bad Request";
    private final static String Unauthorized_SERVER_RESPONSE = "Unauthorized";
    private final static String Unavailable_SERVER_RESPONSE = "Service Unavailable";

    private static final int OK_HTTP_CODE = 200;
    private static final int BadRequest_HTTP_CODE = 400;
    private static final int Unauthorized_HTTP_CODE = 401;
    private static final int Unavailable_HTTP_CODE = 503;


    @Override
    public void start(Future<Void> startFuture) throws Exception {
        /* ################## Authentification ################## */
        jdbcClientConfig = new JsonObject()
                .put("url", "jdbc:mysql://localhost:3306/fhem_userdata?useSSL=false")
                .put("driver_class", "com.mysql.cj.jdbc.Driver")
                .put("initial_pool_size", 5)
                .put("user", "java")
                .put("password", "ialsevlhdakkyllosnmnilk");

        jdbcClient = JDBCClient.createNonShared(vertx, jdbcClientConfig);
        authProvider = JDBCAuth.create(vertx, jdbcClient);
        authHandler = BasicAuthHandler.create(authProvider);
        /* ################## End Authentification ################## */

        Future<SQLConnection> databaseFuture = Future.future();
        jdbcClient.getConnection(databaseFuture.completer());

        /* ################## Routing ################## */
        router = Router.router(getVertx());
        router.exceptionHandler(this::exceptionHandler);
        router.route().handler(BodyHandler.create());
        router.route("/api/*").handler(authHandler);
        router.route(HttpMethod.POST, "/register").handler(this::register);
        router.route(HttpMethod.POST, "/api/setRoomplan").handler(this::setRoomplan);
        router.route(HttpMethod.GET, "/api/setSensorPosition").handler(this::setSensorPosition);
        router.route(HttpMethod.GET, "/api/getModel").handler(this::getModel);
        router.route(HttpMethod.GET, "/api/getPermissions").handler(this::getPermissions);

        router.route(HttpMethod.GET, "/api/getEditMutex").handler(this::getEditMutex);
        router.route(HttpMethod.GET, "/api/getTimeSeries").handler(this::getTimeSeries);
        router.route(HttpMethod.GET, "/api/getRoomplan").handler(this::getRoomplan);
        /* ################## End Routing ################## */


        /* ################## Server ################## */
        server = getVertx().createHttpServer();
        server.requestHandler(router::accept);
        Future<HttpServer> serverFuture = Future.future();
        server.listen(config().getInteger("PORT"), config().getString("HOST"), serverFuture.completer());
        /* ################## End Server ################## */

        CompositeFuture.join(databaseFuture, serverFuture).setHandler(res -> {
            if (res.failed()) {
                startFuture.fail(res.cause());
            } else {
                connection = (SQLConnection) res.result().resultAt(0);
                startFuture.complete();
                System.out.println("Server started successfully!    ");

/*
                Future<Void> testFuture = Future.future();
                Future<Void> testFuture2 = Future.future();
                storeUserInDatabase("hans", "sonne123","Hans","Hut", testFuture);
                storeUserInDatabase("peter", "sterne123","Peter","Lustig", testFuture2);
          */
            }
        });
    }


    private void exceptionHandler(Throwable throwable) {
        throwable.printStackTrace();
    }


    @Override
    public void stop() throws Exception {
        router.clear();
        connection.close();
        server.close();
        super.stop();
    }


    /**
     * handles the REST-Api call for Route /api/register
     * needs parameter username and password
     * optional parameter prename and surname
     * all parameter should be embedded in the request body as Json
     * calls the database to store an new user
     *
     * @param routingContext the context in a route given by the router
     */
    private void register(RoutingContext routingContext) {
        printRequestHeaders(routingContext);
        if (routingContext.getBodyAsJson().getString("username") == null ||
                routingContext.getBodyAsJson().getString("username").isEmpty() ||
                routingContext.getBodyAsJson().getString("password") == null ||
                routingContext.getBodyAsJson().getString("password").isEmpty()) {
            routingContext.response().setStatusCode(BadRequest_HTTP_CODE).end(BadRequest_SERVER_RESPONSE);
            return;
        }
        String username = routingContext.getBodyAsJson().getString("username");
        String password = routingContext.getBodyAsJson().getString("password");
        /* set optional keys to empty string if not given */
        String prename = routingContext.getBodyAsJson().getString("prename") != null
                ? routingContext.getBodyAsJson().getString("prename") : "";
        String surname = routingContext.getBodyAsJson().getString("surname") != null
                ? routingContext.getBodyAsJson().getString("surname") : "";
        Future<Void> databaseWriteFuture = Future.future();
        storeUserInDatabase(username, password, prename, surname, databaseWriteFuture);
        databaseWriteFuture.setHandler(res -> {
            if (databaseWriteFuture.succeeded()) {
                routingContext.response()
                        .setStatusCode(OK_HTTP_CODE)
                        .end(Registered_SERVER_RESPONSE);
            } else {
                routingContext.response()
                        .setStatusCode(BadRequest_HTTP_CODE)
                        .end(BadRequest_SERVER_RESPONSE);
            }
        });
    }


    /**
     * handles the REST-Api call for Route /api/setRoomplan
     * needs parameter room which should be embedded in the request URI
     * needs parameter svg file as text which should be embedded in the request body
     * checks for users permission and calls the model to set the roomplan
     *
     * @param routingContext the context in a route given by the router
     */
    private void setRoomplan(RoutingContext routingContext) {
        printRequestHeaders(routingContext);
        if (routingContext.request().getParam("room") == null
                || routingContext.request().getParam("room").isEmpty()) {
            routingContext.response()
                    .setStatusCode(BadRequest_HTTP_CODE)
                    .end(BadRequest_SERVER_RESPONSE);
            return;
        }
        Future<Boolean> darfErDasFuture = Future.future();
        //TODO: permission string auslagern
        darfErDas(routingContext.user(), "S_Fenster_4", darfErDasFuture.completer());

        darfErDasFuture.setHandler(res -> {
            if (res.succeeded() && darfErDasFuture.result()) {
                //TODO: check body (file) for errors
                String file = routingContext.getBodyAsString();

                vertx.executeBlocking(future -> {
                    Boolean status = parser.setRoomplan(routingContext.request().getParam("room"), file);
                    if (status) {
                        future.handle(Future.succeededFuture(status));
                    } else {
                        future.handle(Future.failedFuture(future.cause()));
                    }
                }, res2 -> {
                    System.out.println("The setRoomplan result is: " + res2.result());
                    if (res2.succeeded()) {
                        routingContext.response()
                                .setStatusCode(OK_HTTP_CODE)
                                .end(ChangedRoomplan_SERVER_RESPONSE);
                    } else {
                        routingContext.response()
                                .setStatusCode(Unavailable_HTTP_CODE)
                                .end(Unavailable_SERVER_RESPONSE);
                    }
                });
            } else {
                routingContext.response().setStatusCode(Unauthorized_HTTP_CODE).end(Unauthorized_SERVER_RESPONSE);
            }
        });
    }


    /**
     * handles the REST-Api call for Route /api/setSensorPosition
     * needs parameter SensorName, coordX and coordY
     * all parameter should be embedded in the request URI
     * checks for users permission and calls the model to set the sensors position
     *
     * @param routingContext the context in a route given by the router
     */
    private void setSensorPosition(RoutingContext routingContext) {
        printRequestHeaders(routingContext);
        /* input validation to prevent SQL injections */
        //TODO: remove?
        routingContext.request().params().forEach(pair -> pair.setValue(
                pair.getValue()
                        .replace("\"", "")
                        .replace(";", "")
                        .replace("\'", "")
                        .replace("\\", "")
        ));
        if (routingContext.request().getParam("SensorName") == null
                || routingContext.request().getParam("SensorName").isEmpty()
                || routingContext.request().getParam("coordX") == null
                || routingContext.request().getParam("coordX").isEmpty()
                || routingContext.request().getParam("coordY") == null
                || routingContext.request().getParam("coordY").isEmpty()) {
            routingContext.response()
                    .setStatusCode(BadRequest_HTTP_CODE)
                    .end(BadRequest_SERVER_RESPONSE);
            return;
        }
        String sensorName = routingContext.request().getParam("SensorName");
        int coordX = Integer.parseInt(routingContext.request().getParam("coordX"));
        int coordY = Integer.parseInt(routingContext.request().getParam("coordY"));

        Future<Boolean> darfErDasFuture = Future.future();
        //TODO: permission string auslagern
        darfErDas(routingContext.user(), "S_Fenster_4", darfErDasFuture.completer());

        darfErDasFuture.setHandler(res -> {
            if (res.succeeded() && darfErDasFuture.result()) {
                vertx.executeBlocking(future -> {
                    Boolean result = parser.setSensorPosition(coordX, coordY, sensorName);
                    if (result) {
                        future.handle(Future.succeededFuture(result));
                    } else {
                        future.handle(Future.failedFuture(future.cause()));
                    }
                }, res2 -> {
                    System.out.println("The setSensorPosition result is: " + res2.result());
                    if (res2.succeeded()) {
                        routingContext.response()
                                .setStatusCode(OK_HTTP_CODE)
                                .end(ChangedSensorPosition_SERVER_RESPONSE);
                    } else {
                        routingContext.response()
                                .setStatusCode(Unavailable_HTTP_CODE)
                                .end(Unavailable_SERVER_RESPONSE);
                    }
                });
            } else {
                routingContext.response().setStatusCode(Unauthorized_HTTP_CODE).end(Unauthorized_SERVER_RESPONSE);
            }
        });
    }


    /**
     * handles the REST-Api call for Route /api/getModel
     * lists the users permission and hands it to the model, to build a user-specific view
     * which is returned as Json in the response body
     *
     * @param routingContext the context in a route given by the router
     */
    private void getModel(RoutingContext routingContext) {
        printRequestHeaders(routingContext);
        Future<List<String>> permissionFuture = Future.future();
        getListOfPermissions(routingContext.user().principal(), permissionFuture);
        permissionFuture.setHandler(res -> {
            if (permissionFuture.succeeded()) {
                List<String> perm = permissionFuture.result();
                vertx.executeBlocking(future -> {
                    Optional<String> answerData_opt = parser.getFHEMModel(perm);
                    if (!answerData_opt.isPresent()) {
                        System.err.println("getModel: AnswerData is not present");
                        future.handle(Future.failedFuture(future.cause()));
                    } else {
                        future.complete(answerData_opt.get());
                    }
                }, res2 -> {
                    if (res2.succeeded()) {
                        String answerData = (String) res2.result();
                        routingContext.response().setStatusCode(OK_HTTP_CODE)
                                .putHeader("content-type", "application/json")
                                .putHeader("content-length", Integer.toString(answerData.length()))
                                .write(answerData)
                                .end(OK_SERVER_RESPONSE);
                    } else {
                        routingContext.response().setStatusCode(Unavailable_HTTP_CODE).end(Unavailable_SERVER_RESPONSE);
                        System.out.println(res.cause());
                    }
                });
            } else {
                routingContext.response().setStatusCode(BadRequest_HTTP_CODE).end(BadRequest_SERVER_RESPONSE);
                System.out.println(res.cause());
            }
        });
    }


    /**
     * handles the REST-Api call for Route /api/getPermissions
     * lists all permissions an user has to a List<String>
     * which is returned as Json in the response body
     *
     * @param routingContext the context in a route given by the router
     */
    private void getPermissions(RoutingContext routingContext) {
        printRequestHeaders(routingContext);
        Future<List<String>> future = Future.future();
        getListOfPermissions(routingContext.user().principal(), future);
        future.setHandler(res -> {
            if (future.succeeded()) {
                List<String> perm = future.result();
                String permString = new Gson().toJson(perm);
                routingContext.response().setStatusCode(OK_HTTP_CODE)
                        .putHeader("content-type", "application/json")
                        .putHeader("content-length", Integer.toString(permString.length()))
                        .write(permString)
                        .end(OK_SERVER_RESPONSE);
            } else {
                routingContext.response().setStatusCode(BadRequest_HTTP_CODE).end(BadRequest_SERVER_RESPONSE);
                System.out.println(res.cause());
                return;
            }
        });
    }


    /**
     * handles the REST-Api call for Route /api/getRoomplan
     * needs parameter room
     * optional parameter hash
     * all parameter should be embedded in the request URI
     * checks for users permission and reads the models room data for the given id
     * which are returned as Json in the response body
     *
     * @param routingContext the context in a route given by the router
     */
    private void getRoomplan(RoutingContext routingContext) {
        printRequestHeaders(routingContext);
        if (routingContext.request().getParam("room") == null
                || routingContext.request().getParam("room").isEmpty()) {
            routingContext.response()
                    .setStatusCode(BadRequest_HTTP_CODE)
                    .end(BadRequest_SERVER_RESPONSE);
            return;
        }
        String room = routingContext.request().getParam("room");
        boolean hasHash;
        long hash;
        if (routingContext.request().getParam("hash") != null
                && !(routingContext.request().getParam("hash").isEmpty())) {
            hash = Long.parseLong(routingContext.request().getParam("hash"));
            hasHash = true;
        } else {
            hash = 0;
            hasHash = false;
        }
        Future<Boolean> darfErDasFuture = Future.future();
        //TODO: permission string auslagern
        darfErDas(routingContext.user(), "S_Fenster_4", darfErDasFuture.completer());
        darfErDasFuture.setHandler(res -> {
            if (res.succeeded() && darfErDasFuture.result()) {
                vertx.executeBlocking(future -> {
                    Optional<String> answerData_opt;
                    if (hasHash) {
                        answerData_opt = parser.getRoomplan(room, hash);
                    } else {
                        answerData_opt = parser.getRoomplan(room);
                    }
                    if (!answerData_opt.isPresent()) {
                        System.err.println("getRoomplan: AnswerData is not present");
                        future.handle(Future.failedFuture(future.cause()));
                    } else {
                        future.complete(answerData_opt.get());
                    }
                }, res2 -> {
                    if (res2.succeeded()) {
                        String answerData = (String) res2.result();
                        routingContext.response().setStatusCode(OK_HTTP_CODE)
                                .putHeader("content-type", "application/json")
                                .putHeader("content-length", Integer.toString(answerData.length()))
                                .write(answerData)
                                .end(OK_SERVER_RESPONSE);
                    } else {
                        routingContext.response().setStatusCode(Unavailable_HTTP_CODE).end(Unavailable_SERVER_RESPONSE);
                        System.out.println(res.cause());
                    }
                });
            } else {
                routingContext.response().setStatusCode(Unauthorized_HTTP_CODE).end(Unauthorized_SERVER_RESPONSE);
            }
        });
    }


    /**
     * handles the REST-Api call for Route /api/getEditMutex
     *
     * @param routingContext the context in a route given by the router
     */
    private void getEditMutex(RoutingContext routingContext) {
        printRequestHeaders(routingContext);
        //TODO: implement
        routingContext.response()
                .setStatusCode(501)
                .end("HelloWorld!");
    }


    /**
     * handles the REST-Api call for Route /api/getTimeSeries
     *
     * @param routingContext the context in a route given by the router
     */
    private void getTimeSeries(RoutingContext routingContext) {
        printRequestHeaders(routingContext);
        //TODO: implement
        //TODO: optional startTime, endTime query param

        routingContext.response()
                .setStatusCode(501)
                .end("HelloWorld!");
    }


    /**
     * checks if an user is authorized to perform an action
     *
     * @param user          the user given by the RoutingContext on a route
     * @param permission    the needed permission, which should already be stored in the database
     * @param resultHandler Handler which gets called, whenever the database action has been finished
     */
    private void darfErDas(User user, String permission, Handler<AsyncResult<Boolean>> resultHandler) {
        Future<List<String>> future = Future.future();
        getListOfPermissions(user.principal(), future.completer());
        future.setHandler(res -> {
            if (future.succeeded()) {
                resultHandler.handle(Future.succeededFuture(future.result().contains(permission)));
            } else {
                System.out.println("Server failed darferdas");
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }


    /**
     * lists all permissions an user has from the database to a List<String>
     *
     * @param user the user given by the RoutingContext on a route
     * @param next Handler which gets called, whenever the database action has been finished
     */
    public void getListOfPermissions(JsonObject user, Handler<AsyncResult<List<String>>> next) {
        String username = user.getString("username");
        if (username == null) {
            next.handle(Future.failedFuture(new IllegalArgumentException("no username specified")));
            return;
        }
        String query = "SELECT perm FROM `ROLE_PERM` INNER JOIN `USER_ROLE` ON `ROLE_PERM`.role=`USER_ROLE`.role where `user`=?";
        JsonArray params = new JsonArray().add(username);
        connection.queryWithParams(query, params, res -> {
            if (res.succeeded()) {
                ResultSet result = res.result();
                List<String> list = result.getRows().stream().map(obj -> obj.getString("perm")).collect(Collectors.toList());
                next.handle(Future.succeededFuture(list));
            } else {
                next.handle(Future.failedFuture(res.cause()));
            }
        });
    }


    /**
     * stores an unregistered user with hashed and salted password to the database
     *
     * @param username the unique username of the new user
     * @param password the clear text password
     * @param prename  the Prename of the new user
     * @param surname  the Surname of the new user
     * @param next     Handler which gets called, whenever the database action has been finished
     */
    private void storeUserInDatabase(String username, String password, String prename, String surname,
                                     Handler<AsyncResult<Void>> next) {
        String salt = authProvider.generateSalt();
        String hash = authProvider.computeHash(password, salt);
        JsonArray params = new JsonArray().add(username).add(hash).add(salt).add(prename).add(surname);
        connection.updateWithParams("INSERT INTO USER VALUES (?, ?, ?, ?, ?)", params, res -> {
            if (res.succeeded()) {
                next.handle(Future.succeededFuture());
            } else {
                next.handle(Future.failedFuture(res.cause()));
            }
        });
    }


    /**
     * prints useful information of an request to the server
     *
     * @param routingContext the context in a route given by the router
     */
    private void printRequestHeaders(RoutingContext routingContext) {
        //TODO: remove debug print
        System.out.println("---");
        System.out.println("Server abs uri: " + routingContext.request().absoluteURI());
        System.out.println("Server params: " + routingContext.request().params());
        System.out.println("Server user: " + routingContext.user().principal().getString("username"));
        routingContext.request().headers().forEach(h -> System.out.println("Server requestHeader: " + h));
    }
}
