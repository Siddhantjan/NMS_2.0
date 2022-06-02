package com.mindarray.nms;

import com.mindarray.Constant;
import com.mindarray.Utils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscoveryEngine extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryEngine.class.getName());

    @Override
    public void start(Promise<Void> startPromise) {

        var eventBus = vertx.eventBus();

        eventBus.<JsonObject>localConsumer(Constant.DISCOVERY_EVENTBUS_ADDRESS, discoveryEventBusHandler -> {
            try {

                var discoveryData = discoveryEventBusHandler.body()
                        .put("category", Constant.DISCOVERY);

                Utils.checkSystemStatus(discoveryData)
                        .compose(future -> Utils.checkPort(discoveryData))
                        .compose(future -> Utils.spawnProcess(discoveryData))
                        .onComplete(futureCompleteHandler -> {

                            if (futureCompleteHandler.succeeded()) {
                                if (futureCompleteHandler.result()
                                        .getString(Constant.STATUS).equals(Constant.SUCCESS)) {
                                    LOG.info(futureCompleteHandler.result().encodePrettily());

                                    LOG.info("Discovery successful");
                                    discoveryEventBusHandler.reply(futureCompleteHandler.result());
                                }else{
                                    LOG.info("Discovery failed");
                                    discoveryEventBusHandler.fail(-1,"discovery failed");

                                }

                            } else {

                                LOG.error("Discovery un-successful");

                                discoveryEventBusHandler.fail(-1,
                                        futureCompleteHandler.cause().getMessage());
                            }
                        });
            }
            catch (Exception exception){
                discoveryEventBusHandler.fail( -1, exception.getCause().getMessage());
            }
        });

        startPromise.complete();
    }
}
