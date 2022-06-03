package com.mindarray.nms;

import com.mindarray.Constant;
import com.mindarray.Utils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class PollerEngine extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(PollerEngine.class.getName());
    private final HashMap<Integer, String> statusCheck = new HashMap<>();

    @Override
    public void start(Promise<Void> startPromise) {
        var eventBus = vertx.eventBus();

        eventBus.<JsonObject>localConsumer(Constant.POLLING_EVENTBUS_ADDRESS, eventBusHandler -> {
            try {

                var pollingData = eventBusHandler.body();

                if (pollingData.getString(Constant.METRIC_GROUP).equals("ping")) {

                    var result = Utils.checkSystemStatus(pollingData);

                    result.onComplete(futureCompleteHandler -> {
                        try {

                            if (futureCompleteHandler.succeeded()) {

                                var data = futureCompleteHandler.result();

                                if (data.containsKey(Constant.STATUS)
                                        &&data.getString(Constant.STATUS).equals(Constant.SUCCESS)) {

                                    statusCheck.put(pollingData.getInteger(Constant.MONITOR_ID), "up");
                                    LOG.info("ping data for ->{}",
                                            pollingData.getInteger(Constant.MONITOR_ID)+ "-"+
                                            statusCheck.get(pollingData.getInteger(Constant.MONITOR_ID)));
                                }
                                else{
                                    statusCheck.put(pollingData.getInteger(Constant.MONITOR_ID), "down");
                                    LOG.error(data.getString(Constant.ERROR));
                                }

                            } else {

                                statusCheck.put(pollingData.getInteger(Constant.MONITOR_ID), "down");
                                LOG.error("ping data ".concat(futureCompleteHandler.cause().getMessage()));

                            }

                        }catch (Exception exception){
                            statusCheck.put(pollingData.getInteger(Constant.MONITOR_ID), "down");
                            LOG.warn("EXCEPTION->{}",exception.getCause().getMessage());


                        }
                    });

                } else {

                    if (!statusCheck.containsKey(pollingData.getInteger(Constant.MONITOR_ID))
                            || statusCheck.get(pollingData.getInteger(Constant.MONITOR_ID)).equals("up")) {

                        var pollingMethod = Utils.spawnProcess(pollingData);

                        pollingMethod.onComplete(pollingCompleteResult -> {
                            try {

                                if (pollingCompleteResult.succeeded()) {

                                    var pollingResult = pollingCompleteResult.result();

                                    if (!pollingResult.containsKey(Constant.ERROR)) {

                                        eventBus.send(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject()
                                                .put("pollingResult", pollingResult)
                                                .put(Constant.METHOD_TYPE, Constant.INSERT_POLLED_DATA));

                                    }
                                    else {
                                        LOG.error(pollingResult.getString(Constant.ERROR));
                                    }

                                } else {

                                    LOG.error(pollingCompleteResult.cause().getMessage());

                                }

                            }catch (Exception exception){

                                LOG.warn("EXCEPTION->{}",exception.getCause().getMessage());

                            }
                        });

                    } else {

                        LOG.error("ping status of monitor ".concat(pollingData.getInteger(Constant.MONITOR_ID)
                                + ":" + statusCheck.get(pollingData.getInteger(Constant.MONITOR_ID))));

                    }
                }
            } catch (Exception exception){
                LOG.warn("EXCEPTION->{}",exception.getCause().getMessage());
            }
        });

        startPromise.complete();
    }
}
