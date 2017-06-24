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
 * serves a stateless REST-Api secured with BasicAuth and backed with JDBC
 * handles user permissions for different actions
 *
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @since 16.06.17.
 */
public class Server extends AbstractVerticle {
    private JDBCAuth authProvider;
    private Router router;
    private HttpServer server;
    private SQLConnection connection;
    private final FHEMParser parser = Main.parser;

    private static final String OK_SERVER_RESPONSE = "OK";
    private static final String Registered_SERVER_RESPONSE = "Registered";
    private static final String ChangedRoomplan_SERVER_RESPONSE = "Changed Roomplan";
    private static final String ChangedSensorPosition_SERVER_RESPONSE = "Changed Sensor Position";
    private static final String BadRequest_SERVER_RESPONSE = "Bad Request";
    private static final String Unauthorized_SERVER_RESPONSE = "Unauthorized";
    private static final String Unavailable_SERVER_RESPONSE = "Service Unavailable";

    private static final int OK_HTTP_CODE = 200;
    private static final int BadRequest_HTTP_CODE = 400;
    private static final int Unauthorized_HTTP_CODE = 401;
    private static final int Unavailable_HTTP_CODE = 503;

    private static final String ContentType_HEADER = "content-type";
    private static final String ContentLength_HEADER = "content-length";
    private static final String ContentType_VALUE = "application/json";

    private static final String SetRoomplan_PERMISSION = "S_Fenster_4";
    private static final String SetSensorPosition_PERMISSION = "S_Fenster_4";
    private static final String GetRoomplan_PERMISSION = "S_Fenster_4";

    private static final String Username_PARAM = "username";
    private static final String Password_PARAM = "password";
    private static final String Prename_PARAM = "prename";
    private static final String Surname_PARAM = "surname";
    private static final String Room_PARAM = "room";
    private static final String SensorName_PARAM = "sensorname";
    private static final String coordX_PARAM = "coordX";
    private static final String coordY_PARAM = "coordY";
    private static final String Hash_PARAM = "hash";
    private static final String TimeSeries_PARAM = "ID";
    private static final String startTime_PARAM = "startTime";
    private static final String endTime_PARAM = "endTime";


    @Override
    public void start(Future<Void> startFuture) throws Exception {
        /* Authentication */
        JsonObject jdbcClientConfig = new JsonObject()
                .put("url", "jdbc:mysql://localhost:3306/fhem_userdata?useSSL=false")
                .put("driver_class", "com.mysql.cj.jdbc.Driver")
                .put("initial_pool_size", 5)
                .put("user", "java")
                .put("password", "ialsevlhdakkyllosnmnilk");
        JDBCClient jdbcClient = JDBCClient.createNonShared(vertx, jdbcClientConfig);
        authProvider = JDBCAuth.create(vertx, jdbcClient);
        AuthHandler authHandler = BasicAuthHandler.create(authProvider);

        /* Database  */
        Future<SQLConnection> databaseFuture = Future.future();
        jdbcClient.getConnection(databaseFuture.completer());

        /* Routing */
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

        /* Server */
        server = getVertx().createHttpServer();
        server.requestHandler(router::accept);
        Future<HttpServer> serverFuture = Future.future();
        server.listen(config().getInteger("PORT"), config().getString("HOST"), serverFuture.completer());

        /* wait for all components to start*/
        CompositeFuture.join(databaseFuture, serverFuture).setHandler(res -> {
            if (res.failed()) {
                startFuture.fail(res.cause());
            } else {
                connection = res.result().resultAt(0);
                startFuture.complete();
                System.out.println("Server started successfully!");
                //TODO: remove debug databse insert
                /*Future<Void> testFuture = Future.future();
                Future<Void> testFuture2 = Future.future();
                storeUserInDatabase("hans", "sonne123","Hans","Hut", testFuture);
                storeUserInDatabase("peter", "sterne123","Peter","Lustig", testFuture2);*/
            }
        });
    }


    private void exceptionHandler(Throwable throwable) {
        throwable.printStackTrace();
    }


    @Override
    public void stop() throws Exception {
        vertx.cancelTimer(Main.parserTimerID);
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
        if (routingContext.getBodyAsJson().getString(Username_PARAM) == null ||
                routingContext.getBodyAsJson().getString(Username_PARAM).isEmpty() ||
                routingContext.getBodyAsJson().getString(Password_PARAM) == null ||
                routingContext.getBodyAsJson().getString(Password_PARAM).isEmpty()) {
            routingContext.response().setStatusCode(BadRequest_HTTP_CODE).end(BadRequest_SERVER_RESPONSE);
            return;
        }
        String username = routingContext.getBodyAsJson().getString(Username_PARAM);
        String password = routingContext.getBodyAsJson().getString(Password_PARAM);
        /* set optional keys to empty string if not given */
        String prename = routingContext.getBodyAsJson().getString(Prename_PARAM) != null
                ? routingContext.getBodyAsJson().getString(Prename_PARAM) : "";
        String surname = routingContext.getBodyAsJson().getString(Surname_PARAM) != null
                ? routingContext.getBodyAsJson().getString(Surname_PARAM) : "";
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
        if (routingContext.request().getParam(Room_PARAM) == null
                || routingContext.request().getParam(Room_PARAM).isEmpty()) {
            routingContext.response()
                    .setStatusCode(BadRequest_HTTP_CODE)
                    .end(BadRequest_SERVER_RESPONSE);
            return;
        }
        Future<Boolean> darfErDasFuture = Future.future();
        darfErDas(routingContext.user(), SetRoomplan_PERMISSION, darfErDasFuture.completer());

        darfErDasFuture.setHandler(res -> {
            if (res.succeeded() && darfErDasFuture.result()) {
                //TODO: check body (file) for errors
                String file = routingContext.getBodyAsString();

                vertx.executeBlocking(future -> {
                    Boolean status = parser.setRoomplan(routingContext.request().getParam(Room_PARAM), file);
                    if (status) {
                        future.handle(Future.succeededFuture(true));
                    } else {
                        future.handle(Future.failedFuture(future.cause()));
                    }
                }, res2 -> {
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
        if (routingContext.request().getParam(SensorName_PARAM) == null
                || routingContext.request().getParam(SensorName_PARAM).isEmpty()
                || routingContext.request().getParam(coordX_PARAM) == null
                || routingContext.request().getParam(coordX_PARAM).isEmpty()
                || routingContext.request().getParam(coordY_PARAM) == null
                || routingContext.request().getParam(coordY_PARAM).isEmpty()) {
            routingContext.response()
                    .setStatusCode(BadRequest_HTTP_CODE)
                    .end(BadRequest_SERVER_RESPONSE);
            return;
        }
        String sensorName = routingContext.request().getParam(SensorName_PARAM);
        int coordX = Integer.parseInt(routingContext.request().getParam(coordX_PARAM));
        int coordY = Integer.parseInt(routingContext.request().getParam(coordY_PARAM));

        Future<Boolean> darfErDasFuture = Future.future();
        darfErDas(routingContext.user(), SetSensorPosition_PERMISSION, darfErDasFuture.completer());

        darfErDasFuture.setHandler(res -> {
            if (res.succeeded() && darfErDasFuture.result()) {
                vertx.executeBlocking(future -> {
                    Boolean result = parser.setSensorPosition(coordX, coordY, sensorName);
                    if (result) {
                        future.handle(Future.succeededFuture(true));
                    } else {
                        future.handle(Future.failedFuture(future.cause()));
                    }
                }, res2 -> {
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
                    //TODO: remove hotfix
                    Optional<String> answerData_opt = Optional.of(parser.getFHEMModel().get().toJson());//getFHEMModel(perm);
                    if (!answerData_opt.isPresent()) {
                        future.handle(Future.failedFuture(future.cause()));
                    } else {
                        future.complete(answerData_opt.get());
                    }
                }, res2 -> {
                    if (res2.succeeded()) {
                        String answerData = (String) res2.result();
                        routingContext.response().setStatusCode(OK_HTTP_CODE)
                                .putHeader(ContentType_HEADER, ContentType_VALUE)
                                .putHeader(ContentLength_HEADER, Integer.toString(answerData.length()))
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
                        .putHeader(ContentType_HEADER, ContentType_VALUE)
                        .putHeader(ContentLength_HEADER, Integer.toString(permString.length()))
                        .write(permString)
                        .end(OK_SERVER_RESPONSE);
            } else {
                routingContext.response().setStatusCode(BadRequest_HTTP_CODE).end(BadRequest_SERVER_RESPONSE);
                System.out.println(res.cause());
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
        if (routingContext.request().getParam(Room_PARAM) == null
                || routingContext.request().getParam(Room_PARAM).isEmpty()) {
            routingContext.response()
                    .setStatusCode(BadRequest_HTTP_CODE)
                    .end(BadRequest_SERVER_RESPONSE);
            return;
        }
        String room = routingContext.request().getParam(Room_PARAM);
        boolean hasHash;
        long hash;
        if (routingContext.request().getParam(Hash_PARAM) != null
                && !(routingContext.request().getParam(Hash_PARAM).isEmpty())) {
            hash = Long.parseLong(routingContext.request().getParam(Hash_PARAM));
            hasHash = true;
        } else {
            hash = 0;
            hasHash = false;
        }
        Future<Boolean> darfErDasFuture = Future.future();
        darfErDas(routingContext.user(), GetRoomplan_PERMISSION, darfErDasFuture.completer());
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
                                .putHeader(ContentType_HEADER, ContentType_VALUE)
                                .putHeader(ContentLength_HEADER, Integer.toString(answerData.length()))
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
     * needs parameter ID
     * optional parameter startTime and endTime
     * all parameter should be embedded in the request URI
     *
     * @param routingContext the context in a route given by the router
     */
    private void getTimeSeries(RoutingContext routingContext) {
        printRequestHeaders(routingContext);
        if (routingContext.request().getParam(TimeSeries_PARAM) == null
                || routingContext.request().getParam(TimeSeries_PARAM).isEmpty()) {
            routingContext.response()
                    .setStatusCode(BadRequest_HTTP_CODE)
                    .end(BadRequest_SERVER_RESPONSE);
            return;
        }
        String ID = routingContext.request().getParam(TimeSeries_PARAM);
        boolean hasTargetTime;
        long startTime;
        long endTime;
        if (routingContext.request().getParam(startTime_PARAM) == null
                || routingContext.request().getParam(startTime_PARAM).isEmpty()
                || routingContext.request().getParam(endTime_PARAM) == null
                || routingContext.request().getParam(endTime_PARAM).isEmpty()) {
            hasTargetTime = false;
            startTime = 0;
            endTime = 0;
        } else {
            startTime = Long.parseLong(routingContext.request().getParam(startTime_PARAM));
            endTime = Long.parseLong(routingContext.request().getParam(endTime_PARAM));
            hasTargetTime = true;
        }
        Future<Boolean> darfErDasFuture = Future.future();
        // TODO: permission check?? no idea about the specific permissions here
        darfErDas(routingContext.user(), "", darfErDasFuture.completer());
        darfErDasFuture.setHandler(res -> {
            if (res.succeeded() && darfErDasFuture.result()) {
                vertx.executeBlocking(future -> {
                    Optional<String> answerData_opt;
                    if (hasTargetTime) {
                        answerData_opt = parser.getTimeserie(startTime, endTime, ID);
                    } else {
                        answerData_opt = parser.getTimeserie(ID);
                    }
                    if (!answerData_opt.isPresent()) {
                        System.err.println("getTimeseries: AnswerData is not present");
                        future.handle(Future.failedFuture(future.cause()));
                    } else {
                        future.complete(answerData_opt.get());
                    }
                }, res2 -> {
                    if (res2.succeeded()) {
                        String answerData = (String) res2.result();
                        routingContext.response().setStatusCode(OK_HTTP_CODE)
                                .putHeader(ContentType_HEADER, ContentType_VALUE)
                                .putHeader(ContentLength_HEADER, Integer.toString(answerData.length()))
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
    private void getListOfPermissions(JsonObject user, Handler<AsyncResult<List<String>>> next) {
        String username = user.getString(Username_PARAM);
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
        System.out.println("Server user: " + routingContext.user().principal().getString(Username_PARAM));
        routingContext.request().headers().forEach(h -> System.out.println("Server requestHeader: " + h));
    }
}
