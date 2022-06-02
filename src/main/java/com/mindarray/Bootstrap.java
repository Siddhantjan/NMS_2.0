package com.mindarray;

import com.mindarray.nms.*;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap {

    public static final Vertx vertx = Vertx.vertx();

    public static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args) {

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (ClassNotFoundException exception) {

            LOG.error(exception.getCause().getMessage());

        }

        start(APIServer.class.getName())

                .compose(future -> start(DatabaseEngine.class.getName()))
                .compose(future -> start(DiscoveryEngine.class.getName()))
                .compose(future->start(MetricScheduler.class.getName()))
                .compose(future -> start(PollerEngine.class.getName()))

                .onComplete(future -> {

                    if (future.succeeded()) {

                        LOG.info("All Verticle deployed successfully");

                    } else {

                        LOG.error(future.cause().getMessage());

                    }
                });
    }
    public static Future<Void> start(String verticle) {

        Promise<Void> promise = Promise.promise();

        vertx.deployVerticle(verticle , handler -> {
            try {

                if (handler.succeeded()) {
                    promise.complete();
                } else {
                    promise.fail(handler.cause());
                }
            }

            catch (Exception exception){

                LOG.warn("EXCEPTION->{}",exception.getCause().getMessage());
            }
        });

        return promise.future();
    }
}
