package com.mindarray;

import com.zaxxer.nuprocess.NuProcess;
import com.zaxxer.nuprocess.NuProcessBuilder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class.getName());

    public static Future<JsonObject> checkSystemStatus(JsonObject credential) {

        Promise<JsonObject> promise = Promise.promise();

        if ((!credential.containsKey(Constant.IP)) || credential.getString(Constant.IP) == null) {

            promise.fail("IP address is null in check system status");

        } else {

            Bootstrap.vertx.executeBlocking(blockingHandler -> {

                try {
                    var processBuilder = new NuProcessBuilder(Arrays
                            .asList("fping", "-c", "3", "-t", "1000", "-q",
                                    credential.getString(Constant.IP)));

                    var handler = new ProcessHandler();

                    processBuilder.setProcessListener(handler);

                    var process = processBuilder.start();


                    process.waitFor(4000, TimeUnit.MILLISECONDS);


                    var result = handler.output();

                    blockingHandler.complete(result);
                } catch (Exception exception) {

                    promise.fail(exception.getMessage());
                    blockingHandler.fail(exception.getMessage());
                }

            }).onComplete(completeHandler -> {

                if (completeHandler.succeeded()) {

                    var result = completeHandler.result().toString();

                    if (result == null) {

                        promise.fail(" Request time out occurred");
                        LOG.error("Request time out occurred");

                    } else {

                        var pattern = Pattern
                                .compile("\\d+.\\d+.\\d+.\\d+\\s*:\\s*\\w+\\/\\w+\\/%\\w+\\s*=\\s*\\d+\\/\\d+\\/(\\d+)%");

                        var matcher = pattern.matcher(result);

                        if ((matcher.find()) && (!matcher.group(1).equals("0"))) {

                            promise.fail("loss percentage is :" + matcher.group(1));

                        } else {

                            promise.complete(credential.put(Constant.STATUS, Constant.SUCCESS));

                        }
                    }

                } else {

                    promise.fail(completeHandler.cause().getMessage());

                }
            });
        }

        return promise.future();
    }

    public static Future<JsonObject> checkPort(JsonObject credential) {

        LOG.info("check port called...");

        Promise<JsonObject> promise = Promise.promise();

        if (credential.getString(Constant.TYPE).equals(Constant.NETWORK)) {

            promise.complete(new JsonObject().put(Constant.STATUS, Constant.SUCCESS));


        } else {

            String ip = credential.getString(Constant.IP);

            int port = credential.getInteger(Constant.PORT);

            try (Socket s = new Socket(ip, port);) {

                promise.complete(new JsonObject().put(Constant.STATUS, Constant.SUCCESS));

            } catch (Exception exception) {
                promise.fail(exception.getMessage());
            }
        }

        return promise.future();
    }

    public static Future<JsonObject> spawnProcess(JsonObject credential) {

        LOG.info("spawn process called...");

        Promise<JsonObject> promise = Promise.promise();

        if (credential == null) {

            LOG.error("credential is null");
            promise.fail("credential is null");

        } else {

                Bootstrap.vertx.<JsonObject>executeBlocking(blockingHandler -> {

                    NuProcess process = null;

                    try {
                        String encoder = (Base64.getEncoder()
                                .encodeToString((credential).toString().getBytes(StandardCharsets.UTF_8)));


                        var processBuilder = new NuProcessBuilder(Arrays.asList("./nms", encoder));
                        var handler = new ProcessHandler();

                        processBuilder.setProcessListener(handler);

                        process = processBuilder.start();

                        process.waitFor(6, TimeUnit.SECONDS);


                        var handlerResult = handler.output();

                        if (handlerResult != null) {

                            blockingHandler.complete(new JsonObject(handlerResult));

                        } else {

                            blockingHandler.fail("request timeout");

                        }
                    } catch (Exception exception) {

                        blockingHandler.fail(exception.getMessage());

                    } finally {

                        if (process != null) {

                            process.destroy(true);

                        }
                    }

                }).onComplete(completeHandler -> {
                    try {
                        if (completeHandler.succeeded()) {
                            if (completeHandler.result() != null
                                    && completeHandler.result().getString(Constant.STATUS).equals(Constant.SUCCESS)) {
                                promise.complete(credential.put(Constant.RESULT, completeHandler.result()));
                            } else {
                                promise.fail(completeHandler.result().getString(Constant.ERROR));
                            }

                        } else {
                            promise.fail(completeHandler.cause().getMessage());
                        }
                    } catch (Exception exception) {
                        LOG.warn("EXCEPTION->{}", exception.getMessage());
                    }
                });



        }

        return promise.future();
    }

    public static Map<String, JsonArray> inputValidation() {

        HashMap<String, JsonArray> inputValidation = new HashMap<>();

        inputValidation.put(Constant.LINUX, new JsonArray().add(Constant.DISCOVERY_ID)
                .add(Constant.DISCOVERY_NAME).add(Constant.CREDENTIAL_PROFILE).add(Constant.TYPE)
                .add(Constant.PORT).add(Constant.IP));

        inputValidation.put(Constant.WINDOWS, new JsonArray().add(Constant.DISCOVERY_ID)
                .add(Constant.DISCOVERY_NAME).add(Constant.CREDENTIAL_PROFILE).add(Constant.TYPE)
                .add(Constant.PORT).add(Constant.IP));

        inputValidation.put(Constant.NETWORK, new JsonArray().add(Constant.DISCOVERY_ID)
                .add(Constant.DISCOVERY_NAME).add(Constant.CREDENTIAL_PROFILE).add(Constant.TYPE)
                .add(Constant.PORT).add(Constant.IP));

        inputValidation.put(Constant.SNMP, new JsonArray().add(Constant.PROTOCOL).add(Constant.CREDENTIAL_NAME)
                .add(Constant.CREDENTIAL_ID).add(Constant.COMMUNITY).add(Constant.VERSION));

        inputValidation.put(Constant.SSH, new JsonArray().add(Constant.PROTOCOL).add(Constant.CREDENTIAL_NAME)
                .add(Constant.CREDENTIAL_ID).add(Constant.USERNAME).add(Constant.PASSWORD));

        inputValidation.put(Constant.POWERSHELL, new JsonArray().add(Constant.PROTOCOL)
                .add(Constant.CREDENTIAL_NAME).add(Constant.CREDENTIAL_ID)
                .add(Constant.USERNAME).add(Constant.PASSWORD));

        return inputValidation;
    }
}
