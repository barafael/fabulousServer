package WebServer;


import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonArray;
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
    public Timeout timeout = Timeout.seconds(500);
    private HttpClientOptions ClientOptions;
    private HttpClient httpClient;
    private Vertx vertx;

    @org.junit.Before
    public void setUp(TestContext testContext) throws Exception {
        int PORT = 8080;
        ClientOptions = new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(PORT);
        vertx = Vertx.vertx();
        httpClient = vertx.createHttpClient(ClientOptions);
        System.out.println("SSH Connection needed for Server");
    }

    @Test
    public void testSetRoomplan(TestContext testContext) {
        final Async async = testContext.async();
        int val = new Random().nextInt(2)+1;
        String authHeader;
        switch (val){
            case 1:
                authHeader = "peter" + ":" + "sterne123";
                break;
            case 2:
                authHeader = "hans" + ":" + "sonne123";
                break;
            default:
                authHeader = "noperm" + ":" + "test";
                break;
        }
        String base64 = "Basic " + new String(Base64.getEncoder().encode(authHeader.getBytes()));
        System.out.println("Client sent [authHeader]: " + base64);
        httpClient.post("/api/setRoomplan?room=room_fablab")
                .putHeader("Authorization", base64)
                .handler(ans -> {
                    ans.headers().forEach(h -> System.out.println("testSetRoomplan_answerHeader: " + h));
                    ans.bodyHandler(body -> {
                        System.out.println("Client received: " + body.toString());
                        async.complete();
                    });
                    testContext.assertEquals(200, ans.statusCode());
                })
                .end("<?xml version=\"1.0\"?><!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"  \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\"><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"467\" height=\"462\">  <rect x=\"80\" y=\"60\" width=\"250\" height=\"250\" rx=\"20\"      style=\"fill:#ff0000; stroke:#000000;stroke-width:2px;\" />    <rect x=\"140\" y=\"120\" width=\"250\" height=\"250\" rx=\"40\"      style=\"fill:#0000ff; stroke:#000000; stroke-width:2px;      fill-opacity:0.7;\" /></svg>");
    }

    @Test
    public void testSetSensorPosition(TestContext testContext) {
        final Async async = testContext.async();
        String authHeader = "hans" + ":" + "sonne123";
        String base64 = "Basic " + new String(Base64.getEncoder().encode(authHeader.getBytes()));
        System.out.println("Client sent [authHeader]: " + base64);
        httpClient.get("/api/setSensorPosition?SensorName=HM_56A439&coordX=44&coordY=2")
                .putHeader("Authorization", base64)
                .handler(ans -> {
                    ans.headers().forEach(h -> System.out.println("testSetSensorPosition_answerHeader: " + h));
                    ans.bodyHandler(body -> {
                        System.out.println("Client received: " + body.toString());//body.toJsonArray());
                        async.complete();
                    });
                    testContext.assertEquals(200, ans.statusCode());
                })
                .end();
    }

    @Test
    public void testGetModel(TestContext testContext) {
        final Async async = testContext.async();
        String authHeader;
        int val = new Random().nextInt(2)+1;
        switch (val){
            case 1:
                authHeader = "peter" + ":" + "sterne123";
                break;
            case 2:
                authHeader = "hans" + ":" + "sonne123";
                break;
            default:
                authHeader = "noperm" + ":" + "test";
                break;
        }
        String base64 = "Basic " + new String(Base64.getEncoder().encode(authHeader.getBytes()));
        System.out.println("Client sent [authHeader]: " + base64);
        httpClient.get("/api/getModel")
                .putHeader("Authorization", base64)
                .handler(ans -> {
                    ans.headers().forEach(h -> System.out.println("testGetModel_answerHeader: " + h));
                    ans.bodyHandler(body -> {
                        System.out.println("Client received: " + body.toString());
                        async.complete();
                    });
                    testContext.assertEquals(200, ans.statusCode());
                })
                .end();
    }

    @Test
    public void testGetModelSmall(TestContext testContext) {
        final Async async = testContext.async();
        String authHeader = "peter:sterne123";
        String base64 = "Basic " + new String(Base64.getEncoder().encode(authHeader.getBytes()));
        System.out.println("Client sent [authHeader]: " + base64);
        httpClient.get("/api/getModel")
                .putHeader("Authorization", base64)
                .handler(ans -> {
                    ans.headers().forEach(h -> System.out.println("testGetModel_answerHeader: " + h));
                    ans.bodyHandler(body -> {
                        System.out.println("Client received: " + body.toString());
                        async.complete();
                    });
                    testContext.assertEquals(200, ans.statusCode());
                })
                .end();
    }

    @Test
    public void testGetTimeSeries(TestContext testContext) {
        final Async async = testContext.async();
        String authHeader = "hans" + ":" + "sonne123";
        String base64 = "Basic " + new String(Base64.getEncoder().encode(authHeader.getBytes()));
        System.out.println("Client sent [authHeader]: " + base64);
        httpClient.get("/api/getTimeSeries?ID=FileLog_HM_521A72_brightness")
                .putHeader("Authorization", base64)
                .handler(ans -> {
                    ans.headers().forEach(h -> System.out.println("testGetTimeSeries_answerHeader: " + h));
                    ans.bodyHandler(body -> {
                        System.out.println("Client received: " + body.toString());
                        async.complete();
                    });
                    testContext.assertEquals(200, ans.statusCode());
                })
                .end();
    }

    @Test
    public void testGetRoomplanWithoutHash(TestContext testContext) {
        final Async async = testContext.async();
        String authHeader = "hans" + ":" + "sonne123";
        String base64 = "Basic " + new String(Base64.getEncoder().encode(authHeader.getBytes()));
        System.out.println("Client sent [authHeader]: " + base64);
        httpClient.get("/api/getRoomplan?room=room_fablab")
                .putHeader("Authorization", base64)
                .handler(ans -> {
                    ans.headers().forEach(h -> System.out.println("testGetRoomplan_answerHeader: " + h));
                    ans.bodyHandler(body -> {
                        System.out.println("Client received: " + body.toString());
                        async.complete();
                    });
                    testContext.assertEquals(200, ans.statusCode());
                })
                .end();
    }

    @Test
    public void testGetRoomplanWithoutHashUnauthorized(TestContext testContext) {
        final Async async = testContext.async();
        String authHeader = "noperms:test";
        String base64 = "Basic " + new String(Base64.getEncoder().encode(authHeader.getBytes()));
        System.out.println("Client sent [authHeader]: " + base64);
        httpClient.get("/api/getRoomplan?room=room_fablab")
                .putHeader("Authorization", base64)
                .handler(ans -> {
                    ans.headers().forEach(h -> System.out.println("testGetRoomplan_answerHeader: " + h));
                    ans.bodyHandler(body -> {
                        System.out.println("Client received: " + body.toString());
                        async.complete();
                    });
                    testContext.assertEquals(401, ans.statusCode());
                })
                .end();
    }

    @Test
    public void testGetRoomplanWithHash(TestContext testContext) {
        final Async async = testContext.async();
        String authHeader = "hans" + ":" + "sonne123";
        String base64 = "Basic " + new String(Base64.getEncoder().encode(authHeader.getBytes()));
        int hash = "thisshallbetheSVG".hashCode();
        System.out.println("Client sent [authHeader]: " + base64);
        httpClient.get("/api/getRoomplan?room=room_fablab&hash=" + hash)
                .putHeader("Authorization", base64)
                .handler(ans -> {
                    ans.headers().forEach(h -> System.out.println("testGetRoomplan_answerHeader: " + h));
                    ans.bodyHandler(body -> {
                        System.out.println("Client received: " + ans.statusMessage());
                        async.complete();
                    });
                    testContext.assertEquals(304, ans.statusCode());
                })
                .end();
    }

    @Test
    public void testGetPermissions(TestContext testContext) {
        final Async async = testContext.async();
        String authHeader;
        int val = new Random().nextInt(2)+1;
        switch (val){
            case 1:
                authHeader = "peter" + ":" + "sterne123";
                break;
            case 2:
                authHeader = "hans" + ":" + "sonne123";
                break;
            default:
                authHeader = "noperm" + ":" + "test";
                break;
        }
        String base64 = "Basic " + new String(Base64.getEncoder().encode(authHeader.getBytes()));
        System.out.println("Client sent [authHeader]: " + base64);
        JsonArray jsonAns = new JsonArray().add("S_Fenster_4");
        httpClient.get("/api/getPermissions")
                .putHeader("Authorization", base64)
                .handler(ans -> {
                    ans.headers().forEach(h -> System.out.println("testGetPermissions_answerHeader: " + h));
                    ans.bodyHandler(body -> {
                        System.out.println("Client received: " + body.toJsonArray());
                        //System.out.println("Client received: [msg, length]: " + body.toJsonObject().toString() + ", " + body.toJsonObject().toString().length());
                        //testContext.assertEquals(body.toJsonArray(), jsonAns);
                        async.complete();
                    });
                    testContext.assertEquals(200, ans.statusCode());
                })
                .end();
    }

    @Test
    public void testRegister(TestContext testContext) {
        final Async async = testContext.async();
        JsonObject json = new JsonObject();
        int rnd = new Random().nextInt(99999);
        json.put("username", "test" + rnd);
        json.put("password", "test" + rnd);
        json.put("prename", "PreTest" + rnd);
        json.put("surname", "SurTest" + rnd);
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
        json.put("prename", "PreTest");
        json.put("surname", "SurTest");
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
