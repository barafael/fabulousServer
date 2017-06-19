package WebServer;


import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Base64;
import java.util.Random;

/**
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @date 16.06.17.
 */

@RunWith(VertxUnitRunner.class)
public class ServerTest {
    @Rule
    public Timeout timeout = Timeout.seconds(50);
    private HttpClientOptions ClientOptions;
    private HttpClient httpClient;
    private JsonObject ServerConfig;
    private DeploymentOptions ServerOptions;
    private Vertx vertx;

    @org.junit.Before
    public void setUp(TestContext testContext) throws Exception {
        vertx = Vertx.vertx();
        int PORT = new Random().nextInt(10000) + 50000;
        System.out.println("Test-PORT: " + PORT);
        ServerConfig = new JsonObject().put("PORT", PORT).put("HOST", "localhost");
        ServerOptions = new DeploymentOptions().setConfig(ServerConfig);
        ClientOptions = new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(8080);//PORT
        //vertx.deployVerticle(Server.class.getCanonicalName(), ServerOptions, testContext.asyncAssertSuccess());
        Main.main(new String[]{""});
        httpClient = vertx.createHttpClient(ClientOptions);
    }

    @Test
    public void testGetData(TestContext testContext) {
        JsonObject json = new JsonObject();
        json.put("data", "[sensor1, sensor2, sensor3]");
        String msg = json.encode();
        System.out.println("Client sent [msg, length]: " + msg + ", " + msg.length());

        String authHeader = "peter:sterne123"; //"hans"+":"+"sonne123";
        String base64 = "Basic " + new String(Base64.getEncoder().encode(authHeader.getBytes()));
        try {
            System.out.println("Server: sleep...");
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Client sent [authHeader]: " + base64);
        final Async async = testContext.async();
        httpClient.get("/api/getSensorData?ID=HM_XYZ")
                .putHeader("Authorization", base64)
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(msg.length()))
                .handler(ans -> {
                    ans.headers().forEach(h -> System.out.println("testGetData_answerHeader: " + h));
                    ans.bodyHandler(body -> {
                        System.out.println("Client received: [msg, length]: " + body.toJsonObject().toString() + ", " + body.toJsonObject().toString().length());
                        async.complete();
                    });
                    testContext.assertEquals(200, ans.statusCode());
                })
                .write(msg)
                .end();
    }

    @Test
    public void testRegister(TestContext testContext) {
        final Async async = testContext.async();
        JsonObject json = new JsonObject();
        int rnd = new Random().nextInt(99999);
        json.put("username", "test"+rnd);
        json.put("password", "test"+rnd);
        json.put("prename","PreTest"+rnd);
        json.put("surname","SurTest"+rnd);
        String msg = json.encode();
        System.out.println("Client sent [msg, length]: " + msg + ", " + msg.length());
        httpClient.post("/register")
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(msg.length()))
                .handler(ans -> {
                    ans.headers().forEach(h -> System.out.println("testRegister_answerHeader: " + h));
                    async.complete();
                    testContext.assertEquals(200, ans.statusCode());
                })
                .write(msg)
                .end();
    }

    @Test
    public void testRegisterFail(TestContext testContext) {
        final Async async = testContext.async();
        JsonObject json = new JsonObject();
        json.put("username", "hans");
        json.put("password", "380");
        json.put("prename","PreTest");
        json.put("surname","SurTest");
        String msg = json.encode();
        System.out.println("Client sent [msg, length]: " + msg + ", " + msg.length());
        httpClient.post("/register")
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(msg.length()))
                .handler(ans -> {
                    ans.headers().forEach(h -> System.out.println("testRegisterFail_answerHeader: " + h));
                    testContext.assertEquals(400, ans.statusCode());
                    async.complete();
                })
                .write(msg)
                .end();
    }

    @Test
    public void testNotAuthorized(TestContext testContext) {
        final Async async = testContext.async();
        httpClient.get("/api/someStuffNotImplemented")
                .handler(ans -> {
                    ans.headers().forEach(h -> System.out.println("testNotAuthorized_answerHeader: " + h));
                    System.out.println("Response status message: " + ans.statusCode() + ": " + ans.statusMessage());
                    testContext.assertEquals(401, ans.statusCode());
                    async.complete();
                })
                .end("trash");
    }


    @Test
    public void testAuthorized(TestContext testContext) {
        final Async async = testContext.async();
        httpClient.get("/api/someStuffNotImplemented")
                .putHeader("Authorization", "Basic aGFuczpzb25uZTEyMw==") // user: hans, password: sonne123 in base64
                .handler(ans -> {
                    ans.headers().forEach(h -> System.out.println("testAuthorized_answerHeader: " + h));
                    System.out.println("Response status message: " + ans.statusCode() + ": " + ans.statusMessage());
                    testContext.assertEquals(404, ans.statusCode());
                    async.complete();
                })
                .end("trash");
    }

    @org.junit.After
    public void tearDown(TestContext testContext) throws Exception {
        Vertx.vertx().close(testContext.asyncAssertSuccess());
    }
}
