package WebServer;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @date 16.06.17.
 */
public final class Main {
    static final JsonObject config = new JsonObject().put("PORT", 8080).put("HOST", "localhost");
    public static final DeploymentOptions options = new DeploymentOptions().setConfig(config);

    public static void main(String[] args){
        System.out.println("Server config: "+options.toJson());
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(Server.class.getCanonicalName(),options);


    }
}
