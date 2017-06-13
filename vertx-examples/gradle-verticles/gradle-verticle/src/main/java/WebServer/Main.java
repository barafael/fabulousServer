package WebServer;

import io.vertx.core.Vertx;

/**
 * Created 09.06.17.
 */
public final class Main {
    public static void main(String[] args){
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(Server.class.getCanonicalName());
    }
}
