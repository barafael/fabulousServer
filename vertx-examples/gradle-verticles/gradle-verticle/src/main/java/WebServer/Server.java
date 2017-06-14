package WebServer;

import io.vertx.core.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        /* ################## Authentification ################## */
        JsonObject jdbcClientConfig = new JsonObject()
                .put("url", "jdbc:mysql://localhost:3306/test?useSSL=false")
                .put("driver_class", "com.mysql.cj.jdbc.Driver")
                .put("initial_pool_size", 5)
                .put("user", "java")
                .put("password", "mydatabasepw");

        JDBCClient jdbcClient = JDBCClient.createNonShared(vertx, jdbcClientConfig);
        authProvider = JDBCAuth.create(vertx, jdbcClient);
        /* ################## End Authentification ################## */

        //TODO: remove future
        Future<UpdateResult> databaseFuture = Future.succeededFuture(); //future()
        // storeInDatabase("karl", "sonne123", authProvider, jdbcClient, databaseFuture);


        /* ################## Routing ################## */
        Router router = Router.router(getVertx());
        /* We need cookies, sessions and request bodies */
        //TODO: edit max body size
        router.route().handler(BodyHandler
                .create()
                .setBodyLimit(500));
        //TODO: session management ?
        //router.route().handler(CookieHandler.create());
        //router.route().handler(SessionHandler.create(LocalSessionStore.create(getVertx())));
        //router.route().handler(UserSessionHandler.create(authProvider));

        AuthHandler authHandler = BasicAuthHandler.create(authProvider);
        router.route("/api/*").handler(authHandler);
        router.route(HttpMethod.POST, "/register").handler(this::register);
        router.route(HttpMethod.GET, "/api/getSensorData").handler(this::getSensorData);
        /* ################## End Routing ################## */


        /* ################## Server ################## */
        HttpServer server = getVertx().createHttpServer();
        server.requestHandler(router::accept);
        Future<HttpServer> serverFuture = Future.future();
        server.listen(config().getInteger("PORT"), serverFuture); //config().getString("HOST"),
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

    @Override
    public void stop() throws Exception {
        //TODO: handle shutdown
        super.stop();
    }

    private void checkPermissions(){

    }

    private void getSensorData(RoutingContext routingContext) {
        System.out.println("Server abs uri: "+routingContext.request().absoluteURI());
        System.out.println("Server params: "+routingContext.request().params());
        System.out.println("Server user: "+routingContext.user().principal());
        routingContext.request().headers().forEach(h -> System.out.println("Server getData_requestHeader: " + h));


        routingContext.user().isAuthorised("commit_code", res -> {
            if (res.succeeded()) {
                boolean hasPermission = res.result();
                System.out.println("Server user action is allowed: "+hasPermission);
            } else {
                // Failed to
                System.out.println("Server user.isAuthorised did not succeed");
            }
        });

        //TODO: call handler to fill answer
        JsonObject sensorData = new JsonObject().put("sensor1","HM_XXXX").put("sensor2","HM_XXXX");
        //String msg = routingContext.getBodyAsJson().put("data", sensorData).toString();
        String msg = "some data";
        HttpServerResponse response = routingContext.response();
        response.setStatusCode(200)
                .putHeader("content-type", "application/json")
                .end(msg);
    }

    private void register(RoutingContext routingContext) {
        //TODO: implement using storeInDatabase(...)
        routingContext.response()
                .setStatusCode(202)
                .end("HelloWorld!");
    }

    private void storeInDatabase(String username, String password, JDBCAuth auth, JDBCClient client,
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
            conn.updateWithParams("INSERT INTO user VALUES (?, ?, ?)", new JsonArray().add(username).add(hash).add(salt),
                    resultHandler);
            conn.close();
        });
    }
}
