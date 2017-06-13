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
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class Server extends AbstractVerticle {


    private JDBCAuth authProvider;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        /* ################## Authentification ################## */
                /*jdbc:mysql://[host1][:port1][,[host2][:port2]]...[/[database]][?propertyName1=propertyValue1[&propertyName2=propertyValue2]...] */
        JsonObject jdbcClientConfig = new JsonObject()
                .put("url", "jdbc:mysql://localhost:3306/test")
                .put("driver_class", "com.mysql.cj.jdbc.Driver")
                .put("initial_pool_size",5)
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
                .setBodyLimit(50));
        router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(getVertx())));
        router.route().handler(UserSessionHandler.create(authProvider));

        // Any requests to URI starting '/private/' require login
        AuthHandler redirectAuthHandler = RedirectAuthHandler.create(authProvider, "/login");
        router.route("/private/*").handler(redirectAuthHandler);
        router.route(HttpMethod.POST, "/login").handler(this::login);
        router.route(HttpMethod.POST, "/register").handler(this::register);
        router.route(HttpMethod.GET, "/private/getData").handler(this::getData);

        // Implement logout
        router.route("/logout").handler(context -> {
            context.clearUser();
            // Redirect back to the index page
            context.response().putHeader("location", "/nothingtoseehere").setStatusCode(302).end("successfully logged out");
        });
        /* ################## End Routing ################## */


        /* ################## Server ################## */
        HttpServer server = getVertx().createHttpServer();
        server.requestHandler(router::accept);
        Future<HttpServer> serverFuture = Future.future();
        server.listen(8080, "localhost", serverFuture);
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

    private void getData(RoutingContext routingContext) {
        String msg = routingContext.getBodyAsJson().put("server", "value").toString();
        HttpServerResponse response = routingContext.response();
        response.setStatusCode(200)
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(msg.length()))
                .end(msg);
    }

    private void login(RoutingContext routingContext) {
        JsonObject input = routingContext.getBodyAsJson();
//        JsonObject authInfo = new JsonObject();
 //       authInfo.put("username", input.getString("username"));
  //      authInfo.put("password", input.getString("password"));
        System.out.println("login input: " + input.toString());
    //    System.out.println("login authInfo: " + authInfo.toString());
       // String clearPassword = input.getString("password");
       // input.put("password",authProvider.);

        authProvider.authenticate(input, res -> {
            if (res.succeeded()) {
                User user = res.result();
                System.out.println("logged in: " + user.toString());
                routingContext.response().setStatusCode(200).end("logged in");
            } else {
                // Failed!
                System.out.println("login failed");
                routingContext.response().setStatusCode(403).end("fail");
            }
        });

    }

    private void register(RoutingContext routingContext) {
        //TODO: implement using storeInDatabase(...)
        routingContext.response()
                .setStatusCode(202)
                .end("HelloWorld!");
    }

    private void storeInDatabase(String username, String password, JDBCAuth auth, JDBCClient client,
                                 Handler<AsyncResult<UpdateResult>> resultHandler) {

        //String salt = auth.generateSalt();
        //String hash = auth.computeHash(password, salt);
        client.getConnection(res -> {
            if (res.failed()) {
                System.err.println("storeInDatabase-FAIL: " + res.cause().getMessage());
                res.cause().printStackTrace();
                resultHandler.handle(Future.failedFuture(res.cause()));
                return;
            }
            SQLConnection conn = res.result();
            System.out.println("DB connection: " + conn.toString());
            conn.updateWithParams("INSERT INTO user VALUES (?, ?, ?)", new JsonArray().add(username).add(password).add("notSalted"),
                    resultHandler);
            //conn.updateWithParams("INSERT INTO user VALUES (?, ?, ?)", new JsonArray().add(username).add(hash).add(salt),
            //        resultHandler);
            conn.close();
        });
    }
}
