package WebServer;

import WebServer.FHEMParser.FHEMParser;
import WebServer.FHEMParser.fhemModel.FHEMModel;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

/**
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @date 16.06.17.
 */
public final class Main {
    static FHEMModel fhemModel;
    static FHEMParser parser = FHEMParser.getInstance();

    static final JsonObject config = new JsonObject().put("PORT", 8080).put("HOST", "localhost");
    public static final DeploymentOptions options = new DeploymentOptions().setConfig(config);

    public static void main(String[] args){
        System.out.println("Server config: "+options.toJson());
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(Server.class.getCanonicalName(),options);

        long parserTimerID = vertx.setPeriodic(20000, id -> {
            Optional<FHEMModel> fhemModel_opt = parser.getFHEMModel();
            if (!fhemModel_opt.isPresent()) {
                System.err.println("FHEM could not parsed.");
                vertx.close();
            }
            fhemModel = fhemModel_opt.get();
        });

    }
}
