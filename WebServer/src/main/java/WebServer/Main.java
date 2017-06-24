package WebServer;

import WebServer.FHEMParser.FHEMParser;
import WebServer.FHEMParser.fhemModel.FHEMModel;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

/**
 * main class to launch the APP-Backend
 *
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @since 16.06.17.
 */
public final class Main {
    static FHEMParser parser = FHEMParser.getInstance();
    static FHEMModel fhemModel;
    static long parserTimerID;

    static {
        Optional<FHEMModel> fhemModel_opt = parser.getFHEMModel();
        if (!fhemModel_opt.isPresent()) {
            System.err.println("FHEM could not be parsed.");
            System.exit(42);
        }
        fhemModel = fhemModel_opt.get();
    }

    /**
     * starts an REST web-server and searches periodically for new data
     *
     * @param args optional argument: the server port
     */
    public static void main(String[] args) {
        int defaultPORT = 8080;
        if (args.length > 0) {
            defaultPORT = Integer.parseInt(args[0]);
        }
        JsonObject config = new JsonObject().put("PORT", defaultPORT).put("HOST", "localhost");
        DeploymentOptions options = new DeploymentOptions().setConfig(config);
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(Server.class.getCanonicalName(), options);

        parserTimerID = vertx.setPeriodic(5000, id -> {
            vertx.executeBlocking(future -> {
                Optional<FHEMModel> fhemModel_opt = parser.getFHEMModel();
                if (!fhemModel_opt.isPresent()) {
                    System.err.println("FHEM could not parsed.");
                    future.handle(Future.failedFuture(future.cause()));
                } else {
                    future.complete(fhemModel_opt.get());
                }
            }, res -> {
                if (res.succeeded()) {
                    fhemModel = (FHEMModel) res.result();
                } else {
                    System.out.println("System exiting: Periodic Parser returned with error!");
                    vertx.close();
                }
            });
        });
    }
}
