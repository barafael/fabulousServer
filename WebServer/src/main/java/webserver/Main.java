package webserver;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import webserver.fhemParser.FHEMParser;
import webserver.fhemParser.fhemModel.FHEMModel;

import java.util.Optional;

/**
 * Main class to launch the APP-Backend.
 *
 * @author Johannes Köstler (github@johanneskoestler.de)
 *         on 16.06.17.
 */
public final class Main {
    public static final Vertx vertx = Vertx.vertx();
    static final FHEMParser parser = FHEMParser.getInstance();
    static long parserTimerID;
    private static FHEMModel fhemModel;

    static {
        Optional<FHEMModel> fhemModel_opt = parser.getFHEMModel();
        if (!fhemModel_opt.isPresent()) {
            System.err.println("FHEM could not be parsed.");
            System.exit(42);
        }
        fhemModel = fhemModel_opt.get();
    }

    /**
     * Starts an REST web-server and searches periodically for new data.
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
                    res.cause().printStackTrace();
                    vertx.cancelTimer(parserTimerID);
                    vertx.close();
                }
            });
        });
    }
}
