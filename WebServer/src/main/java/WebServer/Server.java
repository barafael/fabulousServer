package WebServer;

import WebServer.FHEMParser.FHEMParser;
import WebServer.FHEMParser.fhemModel.FHEMModel;
import com.google.gson.Gson;
import io.vertx.core.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
import java.util.stream.Collectors;

/**
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @date 16.06.17.
 */

// TODO: response.end-Strings in fields auslagern
// TODO: routering mit Future in methode auslagern


public class Server extends AbstractVerticle {
    private JDBCAuth authProvider;
    private Router router;
    private JDBCClient jdbcClient;
    private AuthHandler authHandler;
    private HttpServer server;
    private JsonObject jdbcClientConfig;
    private SQLConnection connection;
    private FHEMParser parser = Main.parser;
    private FHEMModel fhemModel = Main.fhemModel;

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
        // get mutex
        router.route(HttpMethod.GET, "/api/getModel").handler(this::getModel);
        router.route(HttpMethod.GET, "/api/getPermissions").handler(this::getPermissions);
        router.route(HttpMethod.GET, "/api/getTimeSeries").handler(this::getTimeSeries);
        router.route(HttpMethod.GET, "/api/getRooms").handler(this::getRooms);


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
                Future<Boolean> darfErDas = Future.future();
                darfErDas("hans","S_Fenster_3",darfErDas);
                darfErDas.setHandler(res2 -> {
                    if (res2.succeeded() && darfErDas.result()) {
                        System.out.println("er darf das");
                    } else {
                        System.out.println("er darf das nicht");
                    }
                });
            }
        });
    }

    private void exceptionHandler(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void stop() throws Exception {
        router.clear();
        server.close();
        super.stop();
    }

    private void register(RoutingContext routingContext) {
        if (routingContext.getBodyAsJson().getString("username") == null ||
                routingContext.getBodyAsJson().getString("username").isEmpty() ||
                routingContext.getBodyAsJson().getString("password") == null ||
                routingContext.getBodyAsJson().getString("password").isEmpty()) {
            routingContext.response().setStatusCode(400).end("bad request");
            return;
        }

        String username = routingContext.getBodyAsJson().getString("username");
        String password = routingContext.getBodyAsJson().getString("password");
        /* set optional keys to empty string if not given */
        String prename = routingContext.getBodyAsJson().getString("prename") != null ? routingContext.getBodyAsJson().getString("prename") : "";
        String surname = routingContext.getBodyAsJson().getString("surname") != null ? routingContext.getBodyAsJson().getString("surname") : "";

        Future<Void> databaseWriteFuture = Future.future();
        storeUserInDatabase(username, password, prename, surname, databaseWriteFuture);
        databaseWriteFuture.setHandler(res -> {
            if (databaseWriteFuture.succeeded()) {
                routingContext.response()
                        .setStatusCode(200)
                        .end("registered");
            } else {
                routingContext.response()
                        .setStatusCode(400)
                        .end("bad request");
            }
        });
    }

    private void setRoomplan(RoutingContext routingContext) {
        if (routingContext.request().getParam("room") == null || routingContext.request().getParam("room").isEmpty()) {
            routingContext.response()
                    .setStatusCode(400)
                    .end("bad request");
            return;
        }

        Future<Boolean> darfErDasFuture = Future.future();
        //darfErDas(routingContext.user(), "E_Raumänderung", darfErDasFuture.completer());

        darfErDasFuture.setHandler(res -> {
            if (res.succeeded() && darfErDasFuture.result()) {
                //TODO: check body (file) for errors
                String file = routingContext.getBodyAsString();


                //TODO: implement: call setRoomplan in fhemModel with id and svg

                routingContext.response()
                        .setStatusCode(200)
                        .end("room changed");
            } else {
                routingContext.response().setStatusCode(401).end("not authorized");
            }
        });
    }

    private void setSensorPosition(RoutingContext routingContext) {
        /* input validation to prevent SQL injections */
        routingContext.request().params().forEach(pair -> pair.setValue(
                pair.getValue()
                        .replace("\"", "")
                        .replace(";", "")
                        .replace("\'", "")
                        .replace("\\", "")
        ));
        if (routingContext.request().getParam("SensorName") == null || routingContext.request().getParam("SensorName").isEmpty()
                || routingContext.request().getParam("coordX") == null || routingContext.request().getParam("coordX").isEmpty()
                || routingContext.request().getParam("coordY") == null || routingContext.request().getParam("coordY").isEmpty()) {
            routingContext.response()
                    .setStatusCode(400)
                    .end("bad request");
            return;
        }

        //TODO: check user permission

        String sensorName = routingContext.request().getParam("SensorName");
        int coordX = Integer.parseInt(routingContext.request().getParam("coordX"));
        int coordY = Integer.parseInt(routingContext.request().getParam("coordY"));
        vertx.executeBlocking(future -> {
            //TODO: call fhemModel set Sensor Position
            // Call some blocking API that takes a significant amount of time to return
            Boolean result = true;// fhemModel.setSensorPosition(sensorName, coordX, coordY);
            if (result) {
                future.handle(Future.succeededFuture(result));
            } else {
                future.handle(Future.failedFuture(future.cause()));
            }
        }, res -> {
            System.out.println("The setSensorPosition result is: " + res.result());
            if (res.succeeded()) {
                routingContext.response()
                        .setStatusCode(200)
                        .end("changed sensor position");
            } else {
                routingContext.response()
                        .setStatusCode(503)
                        .end("service temporarily not available");
            }
        });
    }

    private void getModel(RoutingContext routingContext) {
        //TODO: remove debug print
        System.out.println("Server abs uri: " + routingContext.request().absoluteURI());
        System.out.println("Server params: " + routingContext.request().params());
        System.out.println("Server user: " + routingContext.user().principal().getString("username"));
        routingContext.request().headers().forEach(h -> System.out.println("Server getModel_requestHeader: " + h));

        Future<List<String>> future = Future.future();
        getListOfPermissions(routingContext.user().principal(), future);
        future.setHandler(res -> {
            if (future.succeeded()) {
                List<String> perm = future.result();
                //TODO: remove hotfix, blocking?
                String answerData = "hotfix"; //parser.getFHEMModel(perm).toJson(); //model.getSubmodel(perm)
                routingContext.response().setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .putHeader("content-length", Integer.toString(answerData.length()))
                        .write(answerData)
                        .end();
            } else {
                routingContext.response().setStatusCode(400).end("bad request");
                System.out.println(res.cause());
                return;
            }
        });
    }

    private void getPermissions(RoutingContext routingContext) {
        Future<List<String>> future = Future.future();
        getListOfPermissions(routingContext.user().principal(), future);
        future.setHandler(res -> {
            if (future.succeeded()) {
                List<String> perm = future.result();
                String permString = new Gson().toJson(perm);
                routingContext.response().setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .putHeader("content-length", Integer.toString(permString.length()))
                        .write(permString)
                        .end();
            } else {
                routingContext.response().setStatusCode(400).end("bad request");
                System.out.println(res.cause());
                return;
            }
        });
    }

    private void getRooms(RoutingContext routingContext) {
        //TODO: implement
        routingContext.response()
                .setStatusCode(501)
                .end("HelloWorld!");
    }

    private void getTimeSeries(RoutingContext routingContext) {
        //TODO: implement


        routingContext.response()
                .setStatusCode(501)
                .end("HelloWorld!");
    }

    //private void darfErDas(User user, String permission, Handler<AsyncResult<Boolean>> resultHandler) {
    private void darfErDas(String user, String permission, Handler<AsyncResult<Boolean>> resultHandler) {
        Future<List<String>> future = Future.future();
        //getListOfPermissions(user.principal(), future.completer());
        getListOfPermissions(new JsonObject().put("username",user), future.completer());
        future.setHandler(res -> {
            if (future.succeeded()) {
                resultHandler.handle(Future.succeededFuture(future.result().contains(permission)));
            } else {
                System.out.println("Server failed darferdas");
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

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
}
