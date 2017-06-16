package WebServer;

import io.vertx.core.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jdbc.JDBCAuth;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.BodyHandler;

public class Server extends AbstractVerticle {


    private JDBCAuth authProvider;
    private Router router;
    private JDBCClient jdbcClient;
    private AuthHandler authHandler;
    private HttpServer server;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        /* ################## Authentification ################## */
        JsonObject jdbcClientConfig = new JsonObject()
                .put("url", "jdbc:mysql://localhost:3306/test?useSSL=false")
                .put("driver_class", "com.mysql.cj.jdbc.Driver")
                .put("initial_pool_size", 5)
                .put("user", "java")
                .put("password", "mydatabasepw");

        jdbcClient = JDBCClient.createNonShared(vertx, jdbcClientConfig);
        authProvider = JDBCAuth.create(vertx, jdbcClient);
        authHandler = BasicAuthHandler.create(authProvider);
        /* ################## End Authentification ################## */

        //TODO: remove
        Future<UpdateResult> databaseFuture = Future.succeededFuture(); //future()
        //storeUserInDatabase("peter", "sterne123","Peter","Lustig", authProvider, jdbcClient, databaseFuture);


        /* ################## Routing ################## */
        router = Router.router(getVertx());
        router.exceptionHandler(this::exceptionHandler);
        router.route().handler(BodyHandler.create());
        router.route("/api/*").handler(authHandler);
        router.route(HttpMethod.POST, "/register").handler(this::register);
        router.route(HttpMethod.POST, "/api/setRoomplan").handler(this::setRoomplan);
        router.route(HttpMethod.POST, "/api/setSensorPosition").handler(this::setSensorPosition);
        router.route(HttpMethod.GET, "/api/getSensorData").handler(this::getSensorData);
        router.route(HttpMethod.GET, "/api/getPermissions").handler(this::getPermissions);
        router.route(HttpMethod.GET, "/api/getRooms").handler(this::getRooms);
        router.route(HttpMethod.GET, "/api/getTimeSeries").handler(this::getTimeSeries);
        /* ################## End Routing ################## */


        /* ################## Server ################## */
        server = getVertx().createHttpServer();
        server.requestHandler(router::accept);
        Future<HttpServer> serverFuture = Future.future();
        //TODO: read hostname from config and  serve only to localnet
        server.listen(config().getInteger("PORT"), config().getString("HOST"), serverFuture); //config().getString("HOST"),
        /* ################## End Server ################## */

        CompositeFuture.join(databaseFuture, serverFuture).setHandler(res -> {
            if (res.failed()) {
                startFuture.fail(res.cause());
            } else {
                startFuture.complete();
                System.out.println("server started");
            }
        });
    }

    private void exceptionHandler(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void stop() throws Exception {
        //TODO: handle shutdown
        super.stop();
        router.clear();
        server.close();
    }

    private void register(RoutingContext routingContext) {
        //TODO: implement using storeInDatabase(...)
        routingContext.response()
                .setStatusCode(202)
                .end("HelloWorld!");
    }

    private void setRoomplan(RoutingContext routingContext) {
        //TODO: implement
        routingContext.response()
                .setStatusCode(202)
                .end("HelloWorld!");
    }

    private void setSensorPosition(RoutingContext routingContext) {
        //TODO: implement
        routingContext.response()
                .setStatusCode(202)
                .end("HelloWorld!");
    }

    private void getSensorData(RoutingContext routingContext) {
        //TODO: check for query param ID, if empty getAll, else get this sensor only
        if (routingContext.request().getParam("ID") == null) {
            // return sensor data from all sensors
        } else {
            //return data from sensor with $ID
        }

        checkPermissions(routingContext.user(), "somePermission");

        //TODO: remove debug print
        System.out.println("Server abs uri: " + routingContext.request().absoluteURI());
        System.out.println("Server params: " + routingContext.request().params());
        System.out.println("Server user: " + routingContext.user().principal());
        routingContext.request().headers().forEach(h -> System.out.println("Server getData_requestHeader: " + h));

        //TODO: call handler to fill answer
        JsonObject sensorData = new JsonObject().put("sensor1", " ").put("sensor2", "HM_XXXX");
        String msg = routingContext.getBodyAsJson().put("data", sensorData).toString();
        HttpServerResponse response = routingContext.response();
        response.setStatusCode(200)
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(msg.length()))
                .write(msg)
                .end();
    }

    private void getPermissions(RoutingContext routingContext) {
        //TODO: implement
        routingContext.response()
                .setStatusCode(202)
                .end("HelloWorld!");
    }

    private void getRooms(RoutingContext routingContext) {
        //TODO: implement
        routingContext.response()
                .setStatusCode(202)
                .end("HelloWorld!");
    }


    private void getTimeSeries(RoutingContext routingContext) {
        //TODO: implement
        routingContext.response()
                .setStatusCode(202)
                .end("HelloWorld!");
    }

    private void checkPermissions(User user, String permission) {
        //TODO: implement
        user.isAuthorised(permission, res -> {
            if (res.succeeded()) {
                boolean hasPermission = res.result();
                System.out.println("Server user action: " + permission + " is allowed: " + hasPermission);
            } else {
                System.out.println("Server user.isAuthorised did not succeed");
                System.out.println(res.cause());
            }
        });
    }

    private void storeUserInDatabase(String username, String password, String prename, String surname, JDBCAuth auth, JDBCClient client,
                                     Handler<AsyncResult<UpdateResult>> resultHandler) {
        String salt = auth.generateSalt();
        String hash = auth.computeHash(password, salt);
        client.getConnection(res -> {
            if (res.failed()) {
                System.err.println("storeInDatabase-FAIL: " + res.cause().getMessage());
                res.cause().printStackTrace();
                resultHandler.handle(Future.failedFuture(res.cause()));
                return;
            }
            SQLConnection conn = res.result();
            System.out.println("DB connection: " + conn.toString());
            conn.updateWithParams("INSERT INTO USER VALUES (?, ?, ?, ?, ?)", new JsonArray().add(username).add(hash).add(salt).add(prename).add(surname),
                    resultHandler);
            conn.close();
        });
    }
}
