package com.mindarray.nms;

import com.mindarray.api.Credential;
import com.mindarray.api.Discovery;
import com.mindarray.api.Metric;
import com.mindarray.api.Monitor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APIServer extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(APIServer.class);

    @Override
    public void start(Promise<Void> startPromise) {

        Router mainRouter = Router.router(vertx);
        Router router = Router.router(vertx);

        mainRouter.mountSubRouter("/api/", router);
        mainRouter.route().handler(BodyHandler.create());

        router.route().handler(BodyHandler.create());

        new Discovery().init(router);
        new Credential().init(router);
        new Monitor().init(router);
        new Metric().init(router);

        vertx.createHttpServer().requestHandler(mainRouter)

                .exceptionHandler(exception -> LOG.error("Exception Occurred".concat(":"
                        + exception.getCause().getMessage())))

                .listen(8080, http -> {

                    if (http.succeeded()) {

                        startPromise.complete();
                        LOG.info("HTTP server started");

                    } else {

                        startPromise.fail(http.cause());

                        LOG.error("HTTP server not started :".concat(http.cause().toString()));

                    }

                });
    }
}
