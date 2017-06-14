package WebServer;


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

/**
 * Created 09.06.17.
 */

@RunWith(VertxUnitRunner.class)
public class ServerTest {
    @Rule
    public Timeout timeout = Timeout.seconds(7);
    private final HttpClientOptions options = new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(8080);
    private HttpClient httpClient;

    @org.junit.Before
    public void setUp(TestContext testContext) throws Exception {
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(Server.class.getCanonicalName(),Main.options, testContext.asyncAssertSuccess());
        httpClient = vertx.createHttpClient(options);
    }

    @Test
    public void testGetData(TestContext testContext) {
        final Async async = testContext.async();
        JsonObject json = new JsonObject();
        json.put("data", "[sensor1, sensor2, sensor3]");
        String msg = json.encode();
        System.out.println("Client sent [msg, length]: " + msg + ", " + msg.length());

        String authHeader = "hans:sonne123"; //"hans"+":"+"sonne123";
        String base64 = "Basic " + new String(Base64.getEncoder().encode(authHeader.getBytes()));
        System.out.println("Client sent [authHeader]: " + base64);
        httpClient.get("/api/getSensorData")
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

    @org.junit.After
    public void tearDown(TestContext testContext) throws Exception {
        Vertx.vertx().close(testContext.asyncAssertSuccess());
    }
}