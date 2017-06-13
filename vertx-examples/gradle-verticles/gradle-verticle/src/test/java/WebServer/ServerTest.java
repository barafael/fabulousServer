package WebServer;


import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created 09.06.17.
 */

@RunWith(VertxUnitRunner.class)
public class ServerTest {
    @Rule
    public Timeout timeout = Timeout.seconds(7);
    private HttpClient httpClient;

    @org.junit.Before
    public void setUp(TestContext testContext) throws Exception {
        final Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(Server.class.getCanonicalName(), testContext.asyncAssertSuccess());
        httpClient = vertx.createHttpClient();
    }

   // @Test
  /*  public void testxy(TestContext testContext) {
        final Async async = testContext.async();
        JsonObject json = new JsonObject();
        json.put("name", "hans");
        String msg = json.encodePrettily();
        httpClient.post(8080, "localhost", "/login/")
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(msg.length()))
                .handler(ans -> {
                    testContext.assertEquals(200, ans.statusCode());
                    testContext.assertEquals("ldfksgadsfjseklfjckabsuebdaxuiw", ans.headers().get("Authentification"));
                    async.complete();
                })
                .write(msg)
                .end();
    }*/
    @Test
    public void testLogin(TestContext testContext){
        final  Async async = testContext.async();
        JsonObject json = new JsonObject();
        json.put("username","karl");
        json.put("password", "sonne123");
        String msg = json.encode();
        System.out.println("Client sent [msg, length]: "+msg+", "+msg.length());
        httpClient.post(8080, "localhost", "/login")
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(msg.length()))
                .handler(ans -> {
                    System.out.println("headers: "+ans.getHeader(""));
                    testContext.assertEquals(200, ans.statusCode());
                    async.complete();
                })
                .write(msg)
                .end();
    }

    @Test
    public void testGetData(TestContext testContext){
        final  Async async = testContext.async();
        JsonObject json = new JsonObject();
        json.put("data","[sensor1, sensor2]");
        String msg = json.encode();
        System.out.println("Client sent [msg, length]: "+msg+", "+msg.length());
        httpClient.get(8080, "localhost", "/private/getData")
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(msg.length()))
                .handler(ans -> {
                    ans.bodyHandler(body -> System.out.println("Client received: [msg, length]: "+body.toJsonObject().toString()+", "+body.toJsonObject().toString().length()));
                    System.out.println("status: "+ans.statusMessage());
                    testContext.assertEquals(200, ans.statusCode());
                    async.complete();
                })
                .write(msg)
                .end();
    }


    @org.junit.After
    public void tearDown(TestContext testContext) throws Exception {
        Vertx.vertx().close(testContext.asyncAssertSuccess());
    }

}