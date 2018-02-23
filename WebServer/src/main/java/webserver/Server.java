package webserver;

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
import webserver.fhemParser.FHEMParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Serves a stateless REST-Api secured with BasicAuth and backed with JDBC.
 * Handles user permissions for different actions.
 *
 * @author Johannes Köstler (github@johanneskoestler.de)
 *         on 16.06.17.
 */
@SuppressWarnings({"WeakerAccess", "ThrowablePrintedToSystemOut"})
public class Server extends AbstractVerticle {
    private static final String OK_SERVER_RESPONSE = "OK";
    private static final String Registered_SERVER_RESPONSE = "Registered";
    private static final String ChangedRoomplan_SERVER_RESPONSE = "Changed Roomplan";
    private static final String ChangedSensorPosition_SERVER_RESPONSE = "Changed Sensor Position";
    private static final String BadRequest_SERVER_RESPONSE = "Bad Request";
    private static final String Unauthorized_SERVER_RESPONSE = "Unauthorized";
    private static final String NotModified_SERVER_RESPONSE = "Not Modified";
    private static final String Unavailable_SERVER_RESPONSE = "Service Unavailable";
    private static final int OK_HTTP_CODE = 200;
    private static final int NotModified_HTTP_CODE = 304;
    private static final int BadRequest_HTTP_CODE = 400;
    private static final int Unauthorized_HTTP_CODE = 401;
    private static final int Unavailable_HTTP_CODE = 503;
    private static final String ContentType_HEADER = "content-type";
    private static final String MutexID_HEADER = "mutexID";
    private static final String ContentType_VALUE = "application/json";
    private static final String Edit_PERMISSION = "E_Änderung";
    private static final String Username_PARAM = "username";
    private static final String MutexID_PARAM = MutexID_HEADER;
    private static final String Password_PARAM = "password";
    private static final String Prename_PARAM = "prename";
    private static final String Surname_PARAM = "surname";
    private static final String Room_PARAM = "room";
    private static final String SensorName_PARAM = "sensorname";
    private static final String State_PARAM = "state";
    private static final String coordX_PARAM = "coordX";
    private static final String coordY_PARAM = "coordY";
    private static final String Hash_PARAM = "hash";
    private static final String Id_PARAM = "ID";
    private static final String startTime_PARAM = "startTime";
    private static final String endTime_PARAM = "endTime";
    private static final String newPassword_HEADER = "rawpw";
    private final FHEMParser parser = Main.PARSER;
    @SuppressWarnings("FieldCanBeLocal")
    private final int DATABASE_KEEP_ALIVE = 15 * 60 * 1000;
    private JDBCAuth authProvider;
    private Router router;
    private HttpServer server;
    private SQLConnection connection;
    private JDBCClient jdbcClient;
    private long TimerofMutexID = 0;
    private long DatabaseAliveTimer = 0;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        /* Authentication */
        JsonObject jdbcClientConfig = new JsonObject()
                .put("url", "jdbc:mysql://localhost:3306/fhem_userdata?useSSL=false")
                .put("driver_class", "com.mysql.cj.jdbc.Driver")
                .put("user", "java")
                .put("password", "ialsevlhdakkyllosnmnilk");
        jdbcClient = JDBCClient.createNonShared(vertx, jdbcClientConfig);
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
        router.route(HttpMethod.GET, "/api/getUserlist").handler(this::getUserlist);
        router.route(HttpMethod.GET, "/api/updatePassword").handler(this::updatePassword);
        router.route(HttpMethod.GET, "/api/getEditMutex").handler(this::getEditMutex);
        router.route(HttpMethod.GET, "/api/releaseEditMutex").handler(this::releaseEditMutex);
        router.route(HttpMethod.GET, "/api/getTimeSeries").handler(this::getTimeSeries);
        router.route(HttpMethod.GET, "/api/getRoomplan").handler(this::getRoomplan);
        router.route(HttpMethod.GET, "/api/setActuator").handler(this::setActuator);
        router.route(HttpMethod.GET, "/api/deleteAccount").handler(this::deleteAccount);

        /* Server */
        server = getVertx().createHttpServer();
        server.requestHandler(router::accept);
        Future<HttpServer> serverFuture = Future.future();
        server.listen(config().getInteger("PORT"), config().getString("HOST"), serverFuture.completer());

        /* wait for all components to start*/
        CompositeFuture.join(databaseFuture, serverFuture).setHandler(res -> {
            if (res.failed()) {
                startFuture.fail(res.cause());
                System.exit(34);
            } else {
                connection = res.result().resultAt(0);
                startFuture.complete();
                System.out.println("Server started successfully!");
                databaseKeepAlive();
            }
        });
    }

    private void databaseKeepAlive() {
        DatabaseAliveTimer = vertx.setPeriodic(DATABASE_KEEP_ALIVE, id -> {
            getListOfPermissions(new JsonObject().put(Username_PARAM, "noperms"), res -> {
                if (!res.succeeded()) {
                    System.err.println("database connection died, trying to reconnect!");
                    /* reconnect Database  */
                    jdbcClient.getConnection(res2 -> {
                        if (res2.failed()) {
                            System.err.println("database seems offline, exiting!");
                            System.exit(12);
                        } else {
                            System.out.println("database successfully reconnected");
                            connection = res2.result();
                        }
                    });
                }
            });
        });
    }

    /**
     * updates an users password in the database
     *
     * @param username    the username of the account
     * @param newPassword the new password to set
     * @param next        Handler which gets called, whenever the database action has been finished
     */
    private void updateUserPassword(String username, String newPassword, Handler<AsyncResult> next) {
        if (username == null || username.isEmpty()) {
            next.handle(Future.failedFuture(new IllegalArgumentException("no username specified")));
            return;
        }
        final String query = "UPDATE `USER` SET `password`=?, `password_salt`=? WHERE `username`=?";
        final String salt = authProvider.generateSalt();
        final String hash = authProvider.computeHash(newPassword, salt);

        final JsonArray params = new JsonArray().add(hash).add(salt).add(username);
        connection.updateWithParams(query, params, res -> {
            if (res.succeeded()) {
                next.handle(Future.succeededFuture());
            } else {
                res.cause().printStackTrace();
                next.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    /**
     * lists all permissions an user has from the database to a List&lt;String&gt;
     *
     * @param user the user given by the RoutingContext on a route
     * @param next Handler which gets called, whenever the database action has been finished
     */
    private void getListOfPermissions(JsonObject user, Handler<AsyncResult<List<String>>> next) {
        final String username = user.getString(Username_PARAM);
        if (username == null) {
            next.handle(Future.failedFuture(new IllegalArgumentException("no username specified")));
            return;
        }
        final String query = "SELECT perm FROM `ROLE_PERM` INNER JOIN `USER_ROLE` ON `ROLE_PERM`.role=`USER_ROLE`.role where `user`=?";
        final JsonArray params = new JsonArray().add(username);
        connection.queryWithParams(query, params, res -> {
            if (res.succeeded()) {
                final ResultSet result = res.result();
                final List<String> list = result.getRows().stream().map(obj -> obj.getString("perm")).collect(Collectors.toList());
                next.handle(Future.succeededFuture(list));
            } else {
                next.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    /**
     * lists all groups an user is participant from the database to a List&lt;String&gt;
     *
     * @param user the user given by the RoutingContext on a route
     * @param next Handler which gets called, whenever the database action has been finished
     */
    private void getListOfGroups(JsonObject user, Handler<AsyncResult<List<String>>> next) {
        final String username = user.getString(Username_PARAM);
        if (username == null) {
            next.handle(Future.failedFuture(new IllegalArgumentException("no username specified")));
            return;
        }
        final String query = "SELECT role FROM `USER_ROLE` WHERE `user`=?";
        final JsonArray params = new JsonArray().add(username);
        connection.queryWithParams(query, params, res -> {
            if (res.succeeded()) {
                final ResultSet result = res.result();
                final List<String> list = result.getRows().stream().map(obj -> obj.getString("role")).collect(Collectors.toList());
                next.handle(Future.succeededFuture(list));
            } else {
                next.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    /**
     * lists all users with their user-, pre- and surnames
     *
     * @param next Handler which gets called, whenever the database action has been finished
     */
    private void getListOfUsers(Handler<AsyncResult<List>> next) {
        final String query = "SELECT prename,surname,username FROM `USER`";
        connection.query(query, res -> {
            if (res.succeeded()) {
                final ResultSet result = res.result();
                List<Object> table = new ArrayList<>();
                result.getResults().forEach(row -> table.add(row.getList()));
                next.handle(Future.succeededFuture(table));
            } else {
                next.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    @Override
    public void stop() throws Exception {
        vertx.cancelTimer(Main.parserTimerID);
        vertx.cancelTimer(DatabaseAliveTimer);
        router.clear();
        connection.close();
        server.close();
        super.stop();
    }

    private void exceptionHandler(Throwable throwable) {
        throwable.printStackTrace();
    }

    /**
     * Handles the REST-API call for Route /api/register.
     * needs parameter username and password
     * optional parameter prename and surname
     * all parameter should be embedded in the request body as Json
     * calls the database to store an new user
     *
     * @param routingContext the context in a route given by the router
     */
    private void register(RoutingContext routingContext) {
        if (Main.SERVER_DBG) printDebugInfo(routingContext);
        final JsonObject body = routingContext.getBodyAsJson();
        final String username = body.getString(Username_PARAM, "");
        final String password = body.getString(Password_PARAM, "");
        final String prename = body.getString(Prename_PARAM, "");
        final String surname = body.getString(Surname_PARAM, "");

        if (username.isEmpty() || password.isEmpty()) {
            routingContext.response().setStatusCode(BadRequest_HTTP_CODE).end(BadRequest_SERVER_RESPONSE);
            return;
        }
        storeUserInDatabase(username, password, prename, surname, res -> {
            if (res.succeeded()) {
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
     * prints useful information of an request to the server
     *
     * @param routingContext the context in a route given by the router
     */
    private void printDebugInfo(RoutingContext routingContext) {
        String username;
        if (routingContext.user() == null) {
            username = "registering NEW user";
        } else {
            username = "User: \"" + routingContext.user().principal().getString(Username_PARAM) + "\"";
        }
        System.out.println("Request: " + routingContext.request().absoluteURI() + " | " + username);
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
        final String salt = authProvider.generateSalt();
        final String hash = authProvider.computeHash(password, salt);
        final JsonArray params = new JsonArray().add(username).add(hash).add(salt).add(prename).add(surname);
        connection.updateWithParams("INSERT INTO USER VALUES (?, ?, ?, ?, ?)", params, res -> {
            if (res.succeeded()) {
                next.handle(Future.succeededFuture());
            } else {
                next.handle(Future.failedFuture(res.cause()));
                res.cause().printStackTrace();
            }
        });
    }

    /**
     * removes an registered user from database
     *
     * @param username the unique username
     * @param next     Handler which gets called, whenever the database action has been finished
     */
    protected void deleteUserFromDatabase(String username, Handler<AsyncResult<Void>> next) {
        final List<String> queries = new ArrayList<>();
        queries.add("START TRANSACTION;");
        queries.add("DELETE FROM `USER_ROLE` WHERE `user`=" + "'" + username + "';");
        queries.add("DELETE FROM `USER`  WHERE `username`=" + "'" + username + "';");
        queries.add("COMMIT;");
        connection.batch(queries, res -> {
            if (res.succeeded()) {
                next.handle(Future.succeededFuture());
            } else {
                next.handle(Future.failedFuture(res.cause()));
                res.cause().printStackTrace();
            }
        });
    }

    /**
     * handles the REST-Api call for Route /api/setRoomplan
     * needs parameter room which should be embedded in the request URI
     * needs parameter png as Base64 which should be embedded in the request body
     * checks for users permission and calls the model to set the roomplan
     *
     * @param routingContext the context in a route given by the router
     */
    private void setRoomplan(RoutingContext routingContext) {
        if (Main.SERVER_DBG) printDebugInfo(routingContext);
        final String room = routingContext.request().getParam(Room_PARAM);
        if (room == null || room.isEmpty()) {
            routingContext.response()
                    .setStatusCode(BadRequest_HTTP_CODE)
                    .end(BadRequest_SERVER_RESPONSE);
            return;
        }
        darfErDas(routingContext.user(), Edit_PERMISSION, res -> {
            if (res.succeeded() && res.result()) {
                final String file = routingContext.getBodyAsString();
                vertx.executeBlocking(future -> {
                    if (!parser.readMutex().equals(routingContext.user().principal().getString(Username_PARAM))) {
                        future.handle(Future.failedFuture(future.cause()));
                    } else {
                        final Boolean status = parser.setRoomplan(room, file);
                        if (status) {
                            future.handle(Future.succeededFuture(true));
                        } else {
                            future.handle(Future.failedFuture(future.cause()));
                        }
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
     * checks if an user is authorized to perform an action
     *
     * @param user          the user given by the RoutingContext on a route
     * @param permission    the needed permission, which should already be stored in the database
     * @param resultHandler Handler which gets called, whenever the database action has been finished
     */
    private void darfErDas(User user, String permission, Handler<AsyncResult<Boolean>> resultHandler) {
        getListOfPermissions(user.principal(), res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(res.result().contains(permission)));
            } else {
                System.out.println("Server failed darferdas");
                resultHandler.handle(Future.failedFuture(res.cause()));
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
        if (Main.SERVER_DBG) printDebugInfo(routingContext);
        final String sensorName = routingContext.request().getParam(SensorName_PARAM);
        final String coordX_string = routingContext.request().getParam(coordX_PARAM);
        final String coordY_string = routingContext.request().getParam(coordY_PARAM);
        if (sensorName == null
                || sensorName.isEmpty()
                || coordX_string == null
                || coordX_string.isEmpty()
                || coordY_string == null
                || coordY_string.isEmpty()) {
            routingContext.response()
                    .setStatusCode(BadRequest_HTTP_CODE)
                    .end(BadRequest_SERVER_RESPONSE);
            return;
        }
        final int coordX = Integer.parseInt(coordX_string);
        final int coordY = Integer.parseInt(coordY_string);

        darfErDas(routingContext.user(), Edit_PERMISSION, res -> {
            if (res.succeeded() && res.result()) {
                vertx.executeBlocking(future -> {
                    if (!parser.readMutex().equals(routingContext.user().principal().getString(Username_PARAM))) {
                        future.handle(Future.failedFuture(future.cause()));
                    } else {
                        final Boolean result = parser.setSensorPosition(coordX, coordY, sensorName);
                        if (result) {
                            future.handle(Future.succeededFuture());
                        } else {
                            future.handle(Future.failedFuture(future.cause()));
                        }
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
        if (Main.SERVER_DBG) printDebugInfo(routingContext);
        final JsonObject user = routingContext.user().principal();
        Future<List<String>> permissionsFuture = Future.future();
        getListOfPermissions(user, permissionsFuture);
        Future<List<String>> groupsFuture = Future.future();
        getListOfGroups(user, groupsFuture);
        CompositeFuture.join(permissionsFuture, groupsFuture).setHandler(res -> {
            if (res.succeeded()) {
                final List<String> permissions = res.result().resultAt(0);
                final List<String> groups = res.result().resultAt(1);
                vertx.executeBlocking(future -> {
                    final Optional<String> answerData_opt = parser.getFHEMModelJSON(permissions, groups);
                    if (!answerData_opt.isPresent()) {
                        System.out.println("Server getModel: answerData is not present");
                        future.handle(Future.failedFuture(future.cause()));
                    } else {
                        future.handle(Future.succeededFuture(answerData_opt.get()));
                    }
                }, res2 -> {
                    if (res2.succeeded()) {
                        final String answerData = (String) res2.result();
                        if (!answerData.equals("null")) {
                            routingContext.response().setStatusCode(OK_HTTP_CODE)
                                    .putHeader(ContentType_HEADER, ContentType_VALUE)
                                    .end(answerData);
                        } else {
                            routingContext.response().setStatusCode(Unauthorized_HTTP_CODE).end(Unauthorized_SERVER_RESPONSE);
                        }
                    } else {
                        routingContext.response().setStatusCode(Unavailable_HTTP_CODE).end(Unavailable_SERVER_RESPONSE);
                        System.out.println(res2.cause().getMessage());
                    }
                });
            } else {
                routingContext.response().setStatusCode(Unavailable_HTTP_CODE).end(Unavailable_SERVER_RESPONSE);
                System.out.println(res.cause().getMessage());
            }
        });
    }

    /**
     * handles the REST-Api call for Route /api/getPermissions
     * lists all permissions an user has to a List&lt;String&gt;
     * which is returned as Json in the response body
     *
     * @param routingContext the context in a route given by the router
     */
    private void getPermissions(RoutingContext routingContext) {
        if (Main.SERVER_DBG) printDebugInfo(routingContext);
        final JsonObject user = routingContext.user().principal();
        Future<List<String>> permissionsFuture = Future.future();
        getListOfPermissions(user, permissionsFuture);
        Future<List<String>> groupsFuture = Future.future();
        getListOfGroups(user, groupsFuture);
        CompositeFuture.join(permissionsFuture, groupsFuture).setHandler(res -> {
            if (res.succeeded()) {
                final List<String> permissions = res.result().resultAt(0);
                final List<String> groups = res.result().resultAt(1);
                final HashMap<String, List<String>> answer = new HashMap<>();
                answer.put("permissions", permissions);
                answer.put("groups", groups);
                final String answerString = new Gson().toJson(answer);
                routingContext.response().setStatusCode(OK_HTTP_CODE)
                        .putHeader(ContentType_HEADER, ContentType_VALUE)
                        .end(answerString);
            } else {
                routingContext.response().setStatusCode(BadRequest_HTTP_CODE).end(BadRequest_SERVER_RESPONSE);
                System.out.println(res.cause().getMessage());
            }
        });
    }

    /**
     * handles the REST-Api call for Route /api/getUserlist
     * lists all users with their user-, pre- and surname;
     * which is returned as Json in the response body
     *
     * @param routingContext the context in a route given by the router
     */
    private void getUserlist(RoutingContext routingContext) {
        if (Main.SERVER_DBG) printDebugInfo(routingContext);
        darfErDas(routingContext.user(), Edit_PERMISSION, res -> {
            if (res.succeeded() && res.result()) {
                getListOfUsers(res2 -> {
                    if (res2.succeeded()) {
                        final String answerString = new Gson().toJson(res2.result());
                        routingContext.response().setStatusCode(OK_HTTP_CODE)
                                .putHeader(ContentType_HEADER, ContentType_VALUE)
                                .end(answerString);
                    } else {
                        routingContext.response().setStatusCode(Unavailable_HTTP_CODE).end(Unavailable_SERVER_RESPONSE);
                        System.out.println(res2.cause().getMessage());
                    }
                });
            } else {
                routingContext.response().setStatusCode(Unauthorized_HTTP_CODE).end(Unauthorized_SERVER_RESPONSE);
                System.out.println(res.cause().getMessage());
            }
        });
    }


    /**
     * handles the REST-Api call for Route /api/updatePassword
     * needs header 'rawpw' containing the new password
     * optional parameter username
     *
     * @param routingContext the context in a route given by the router
     */
    private void updatePassword(RoutingContext routingContext) {
        if (Main.SERVER_DBG) printDebugInfo(routingContext);
        final String requestingUserName = routingContext.user().principal().getString(Username_PARAM);
        String toUpdateUserName = routingContext.request().getParam(Username_PARAM);
        final String newPassword = routingContext.request().headers().get(newPassword_HEADER);

        if (newPassword == null || newPassword.isEmpty()) {
            routingContext.response().setStatusCode(BadRequest_HTTP_CODE).end(BadRequest_SERVER_RESPONSE);
            return;
        }
        if (toUpdateUserName == null || toUpdateUserName.isEmpty()) {
            // update own account
            updateUserPassword(requestingUserName, newPassword, asyncResult -> {
                if (asyncResult.succeeded()) {
                    routingContext.response().setStatusCode(OK_HTTP_CODE).end(OK_SERVER_RESPONSE);
                } else {
                    routingContext.response().setStatusCode(Unavailable_HTTP_CODE).end(Unavailable_SERVER_RESPONSE);
                    asyncResult.cause().printStackTrace();
                }
            });
        } else {
            // check permissions
            darfErDas(routingContext.user(), Edit_PERMISSION, res -> {
                if (res.succeeded() && res.result()) {
                    updateUserPassword(toUpdateUserName, newPassword, asyncResult -> {
                        if (asyncResult.succeeded()) {
                            routingContext.response().setStatusCode(OK_HTTP_CODE).end(OK_SERVER_RESPONSE);
                        } else {
                            routingContext.response().setStatusCode(Unavailable_HTTP_CODE).end(Unavailable_SERVER_RESPONSE);
                            asyncResult.cause().printStackTrace();
                        }
                    });
                } else {
                    routingContext.response().setStatusCode(Unauthorized_HTTP_CODE).end(Unauthorized_SERVER_RESPONSE);
                }
            });
        }
    }

    /**
     * handles the REST-Api call for Route /api/deleteAccount
     * optional parameter username
     *
     * @param routingContext the context in a route given by the router
     */
    private void deleteAccount(RoutingContext routingContext) {
        if (Main.SERVER_DBG) printDebugInfo(routingContext);
        final String requestingUserName = routingContext.user().principal().getString(Username_PARAM);
        String toDeleteUserName = routingContext.request().getParam(Username_PARAM);
        if (toDeleteUserName == null || toDeleteUserName.isEmpty()) {
            // delete own account
            deleteUserFromDatabase(requestingUserName, asyncResult -> {
                if (asyncResult.succeeded()) {
                    routingContext.response().setStatusCode(OK_HTTP_CODE).end(OK_SERVER_RESPONSE);
                } else {
                    routingContext.response().setStatusCode(Unavailable_HTTP_CODE).end(Unavailable_SERVER_RESPONSE);
                    asyncResult.cause().printStackTrace();
                }
            });
        } else {
            darfErDas(routingContext.user(), Edit_PERMISSION, res -> {
                if (res.succeeded() && res.result()) {
                    deleteUserFromDatabase(toDeleteUserName, asyncResult -> {
                        if (asyncResult.succeeded()) {
                            routingContext.response().setStatusCode(OK_HTTP_CODE).end(OK_SERVER_RESPONSE);
                        } else {
                            routingContext.response().setStatusCode(Unavailable_HTTP_CODE).end(Unavailable_SERVER_RESPONSE);
                            asyncResult.cause().printStackTrace();
                        }
                    });
                } else {
                    routingContext.response().setStatusCode(Unauthorized_HTTP_CODE).end(Unauthorized_SERVER_RESPONSE);
                }
            });
        }
    }

    /**
     * handles the REST-Api call for Route /api/getRoomplan
     * needs parameter room
     * optional parameter hash
     * all parameter should be embedded in the request URI
     * checks for users permission and reads the roomplan for the given id
     * which is returned as Base64 in the response body
     *
     * @param routingContext the context in a route given by the router
     */
    private void getRoomplan(RoutingContext routingContext) {
        if (Main.SERVER_DBG) printDebugInfo(routingContext);
        final String room = routingContext.request().getParam(Room_PARAM);
        if (room == null || room.isEmpty()) {
            routingContext.response()
                    .setStatusCode(BadRequest_HTTP_CODE)
                    .end(BadRequest_SERVER_RESPONSE);
            return;
        }
        final boolean hasHash;
        final long hash;
        final String hash_string = routingContext.request().getParam(Hash_PARAM);
        if (hash_string == null || hash_string.isEmpty()) {
            hash = 0;
            hasHash = false;
        } else {
            hash = Long.parseLong(hash_string);
            hasHash = true;
        }
        getListOfPermissions(routingContext.user().principal(), res -> {
            if (res.succeeded()) {
                final List<String> perm = res.result();
                vertx.executeBlocking(future -> {
                    final Optional<String> answerData_opt;
                    if (hasHash) {
                        answerData_opt = parser.getRoomplan(room, hash, perm);
                    } else {
                        answerData_opt = parser.getRoomplan(room, perm);
                    }
                    if (!answerData_opt.isPresent()) {
                        if (Main.SERVER_DBG) System.err.println("getRoomplan: AnswerData is not present");
                        future.handle(Future.failedFuture(future.cause()));
                    } else {
                        future.handle(Future.succeededFuture(answerData_opt.get()));
                    }
                }, res2 -> {
                    if (res2.succeeded()) {
                        final String answerData = (String) res2.result();
                        if (!answerData.equals("null")) {
                            routingContext.response().setStatusCode(OK_HTTP_CODE)
                                    .putHeader(ContentType_HEADER, ContentType_VALUE)
                                    .end(answerData);
                        } else {
                            routingContext.response().setStatusCode(Unauthorized_HTTP_CODE).end(Unauthorized_SERVER_RESPONSE);
                        }
                    } else {
                        routingContext.response().setStatusCode(NotModified_HTTP_CODE).end(NotModified_SERVER_RESPONSE);
                    }
                });
            } else {
                routingContext.response().setStatusCode(Unavailable_HTTP_CODE).end(Unavailable_SERVER_RESPONSE);
                System.out.println(res.cause().getMessage());
            }
        });
    }

    /**
     * handles the REST-Api call for Route /api/getEditMutex
     * reserves the right to edit the data model for one user
     *
     * @param routingContext the context in a route given by the router
     */
    private void getEditMutex(RoutingContext routingContext) {
        if (Main.SERVER_DBG) printDebugInfo(routingContext);
        darfErDas(routingContext.user(), Edit_PERMISSION, res -> {
            if (res.succeeded() && res.result()) {
                vertx.executeBlocking(future -> {
                    Optional<Long> result = parser.getMutex(routingContext.user().principal().getString(Username_PARAM));
                    if (result.isPresent()) {
                        future.handle(Future.succeededFuture(result.get()));
                    } else {
                        future.handle(Future.failedFuture(future.cause()));
                    }
                }, res2 -> {
                    if (res2.succeeded()) {
                        TimerofMutexID = (Long) res2.result();
                        if (Main.SERVER_DBG) System.out.println("set TimerofMutexID to: " + TimerofMutexID);
                        final String str = res2.result().toString();
                        routingContext.response()
                                .putHeader(MutexID_HEADER, str)
                                .setStatusCode(OK_HTTP_CODE)
                                .end(OK_SERVER_RESPONSE);
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
     * handles the REST-Api call for Route /api/releaseEditMutex
     * removes the right to edit the data model from the user
     * needs parameter ID
     * all parameter should be embedded in the request URI
     *
     * @param routingContext the context in a route given by the router
     */
    private void releaseEditMutex(RoutingContext routingContext) {
        if (Main.SERVER_DBG) printDebugInfo(routingContext);
        final String mutexID = routingContext.request().getParam(MutexID_PARAM);
        if (mutexID == null || mutexID.isEmpty()) {
            routingContext.response()
                    .setStatusCode(BadRequest_HTTP_CODE)
                    .end(BadRequest_SERVER_RESPONSE);
            return;
        }
        final long timerID = Long.parseLong(mutexID);
        vertx.executeBlocking(future -> {
            if (timerID != TimerofMutexID) {
                if (Main.SERVER_DBG)
                    System.out.println("Server not matching: timerID=" + timerID + ", TimerofMutexID=" + TimerofMutexID);
                routingContext.response()
                        .setStatusCode(Unavailable_HTTP_CODE)
                        .end(Unavailable_SERVER_RESPONSE);
                return;
            }
            final boolean result = parser.releaseMutex(routingContext.user().principal().getString(Username_PARAM));
            if (result) {
                future.handle(Future.succeededFuture());
            } else {
                future.handle(Future.failedFuture(future.cause()));
            }
        }, res2 -> {
            if (res2.succeeded()) {
                vertx.cancelTimer(timerID);
                TimerofMutexID = 0;
                if (Main.SERVER_DBG) System.out.println("Server canceled timer and reset TimerofMutexID");
                routingContext.response()
                        .setStatusCode(OK_HTTP_CODE)
                        .end(OK_SERVER_RESPONSE);
            } else {
                routingContext.response()
                        .setStatusCode(Unavailable_HTTP_CODE)
                        .end(Unavailable_SERVER_RESPONSE);
            }
        });
    }

    /**
     * handles the REST-Api call for Route /api/setActuator
     * needs parameter sensorname, state
     * all parameter should be embedded in the request URI
     *
     * @param routingContext the context in a route given by the router
     */

    private void setActuator(RoutingContext routingContext) {
        if (Main.SERVER_DBG) printDebugInfo(routingContext);
        final String sensorName = routingContext.request().getParam(SensorName_PARAM);
        final String state_param = routingContext.request().getParam(State_PARAM);
        if (sensorName == null || sensorName.isEmpty()
                || state_param == null || state_param.isEmpty()) {
            routingContext.response()
                    .setStatusCode(BadRequest_HTTP_CODE)
                    .end(BadRequest_SERVER_RESPONSE);
            return;
        }

        final boolean state = Boolean.parseBoolean(state_param);

        getListOfPermissions(routingContext.user().principal(), res -> {
            if (res.succeeded()) {
                List<String> perm = res.result();
                vertx.executeBlocking(future -> {
                    Boolean status = parser.setActuator(sensorName, state, perm);
                    if (status) {
                        future.handle(Future.succeededFuture(true));
                    } else {
                        future.handle(Future.failedFuture(future.cause()));
                    }
                }, res2 -> {
                    if (res2.succeeded()) {
                        routingContext.response()
                                .setStatusCode(OK_HTTP_CODE)
                                .end(OK_SERVER_RESPONSE);
                    } else {
                        routingContext.response()
                                .setStatusCode(NotModified_HTTP_CODE)
                                .end(NotModified_SERVER_RESPONSE);
                    }
                });
            } else {
                routingContext.response().setStatusCode(Unavailable_HTTP_CODE).end(Unavailable_SERVER_RESPONSE);
                System.out.println(res.cause().getMessage());
            }
        });
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
        if (Main.SERVER_DBG) printDebugInfo(routingContext);
        final String id_param = routingContext.request().getParam(Id_PARAM);
        final String startTime_param = routingContext.request().getParam(startTime_PARAM);
        final String endTime_param = routingContext.request().getParam(endTime_PARAM);

        if (id_param == null || id_param.isEmpty()) {
            routingContext.response()
                    .setStatusCode(BadRequest_HTTP_CODE)
                    .end(BadRequest_SERVER_RESPONSE);
            return;
        }
        final boolean hasTargetTime;
        final long endTime;
        final long startTime;
        if (startTime_param == null || startTime_param.isEmpty()
                || endTime_param == null || endTime_param.isEmpty()) {
            hasTargetTime = false;
            startTime = 0;
            endTime = 0;
        } else {
            startTime = Long.parseLong(startTime_param);
            endTime = Long.parseLong(endTime_param);
            hasTargetTime = true;
        }
        getListOfPermissions(routingContext.user().principal(), res -> {
            if (res.succeeded()) {
                vertx.executeBlocking(future -> {
                    List<String> perm = res.result();
                    Optional<String> answerData_opt;
                    if (hasTargetTime) {
                        answerData_opt = parser.getTimeserie(startTime, endTime, id_param, perm);
                    } else {
                        answerData_opt = parser.getTimeserie(id_param, perm);
                    }
                    if (answerData_opt.isPresent()) {
                        future.handle(Future.succeededFuture(answerData_opt.get()));
                    } else {
                        System.err.println("getTimeseries: AnswerData is not present");
                        future.handle(Future.failedFuture(future.cause()));
                    }
                }, res2 -> {
                    if (res2.succeeded()) {
                        String answerData = (String) res2.result();
                        routingContext.response().setStatusCode(OK_HTTP_CODE)
                                .putHeader(ContentType_HEADER, ContentType_VALUE)
                                .end(answerData);
                    } else {
                        routingContext.response().setStatusCode(Unauthorized_HTTP_CODE).end(Unauthorized_SERVER_RESPONSE);
                        System.out.println(res2.cause().getMessage());
                    }
                });
            } else {
                routingContext.response().setStatusCode(Unavailable_HTTP_CODE).end(Unavailable_SERVER_RESPONSE);
                System.out.println(res.cause().getMessage());
            }
        });
    }
}
