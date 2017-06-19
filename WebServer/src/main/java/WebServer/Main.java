package WebServer;

import WebServer.FHEMParser.FHEMParser;
import WebServer.FHEMParser.fhemModel.FHEMModel;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @date 16.06.17.
 */
public final class Main {
    static FHEMParser parser = FHEMParser.getInstance();
    static JsonObject config;
    static DeploymentOptions options;
    static FHEMModel fhemModel;
    static {
        Optional<FHEMModel> fhemModel_opt = parser.getFHEMModel();
        if (!fhemModel_opt.isPresent()) {
            System.err.println("FHEM could not be parsed.");
            System.exit(1337);
        }
        fhemModel = fhemModel_opt.get();
    }

    public static void main(String[] args){
        System.out.println("Server args: "+ Arrays.toString(args));
        int port = 8080;
        if (args.length > 0){
            port = Integer.parseInt(args[0]);
            System.out.println("Server port: "+port);
        }
        config = new JsonObject().put("PORT",port).put("HOST", "localhost");
        options = new DeploymentOptions().setConfig(config);
        System.out.println("Server config: "+options.toJson());
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(Server.class.getCanonicalName(),options);

        long parserTimerID = vertx.setPeriodic(5000, id -> {
            Optional<FHEMModel> fhemModel_opt = parser.getFHEMModel();
            if (!fhemModel_opt.isPresent()) {
                System.err.println("FHEM could not parsed.");
                vertx.close();
            }
            fhemModel = fhemModel_opt.get();
        });

    }
}
