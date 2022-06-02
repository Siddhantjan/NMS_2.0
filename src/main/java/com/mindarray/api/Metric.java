package com.mindarray.api;

import com.mindarray.Bootstrap;
import com.mindarray.Constant;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static java.lang.Math.max;

public class Metric {

    private final EventBus eventBus = Bootstrap.vertx.eventBus();


    private static final Logger LOG = LoggerFactory.getLogger(Metric.class.getName());

    public void init(Router router) {

        LOG.info("metric init fn() called");

        router.route().method(HttpMethod.PUT).path(Constant.METRIC_POINT + "/:id")
                .handler(this::validate).handler(this::update);

        router.route().method(HttpMethod.GET).path(Constant.METRIC_POINT).handler(this::get);


        router.route().method(HttpMethod.GET).path(Constant.METRIC_POINT + "/:id")
                .handler(this::validate).handler(this::getByID);

        router.route().method(HttpMethod.GET).path(Constant.METRIC_POINT + "/limit/:id")
                .handler(this::validate).handler(this::getPollingData);

    }

    private void getByID(RoutingContext context) {

        LOG.info("metric get by id fn() called");
        try {

            String query = "";
            var type = context.queryParam("type");

            if (!type.isEmpty() && type.get(0).equals("monitor")) {

                query = "select * from metric where monitor_id=" + context.pathParam(Constant.ID) + ";";

            } else {

                query = "select * from metric where id=" + context.pathParam(Constant.ID) + ";";
            }

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject()
                            .put(Constant.METHOD_TYPE, Constant.GET_DATA).put(Constant.QUERY, query),
                    eventBusHandler -> {

                        try {

                            if (eventBusHandler.succeeded()) {
                                var result = eventBusHandler.result().body().getJsonArray(Constant.DATA);

                                if (!result.isEmpty()) {

                                    response(context, 200, new JsonObject()
                                            .put(Constant.STATUS, Constant.SUCCESS)
                                            .put(Constant.RESULT, result));

                                    LOG.info("context ->{}, status ->{}", context.pathParam(Constant.ID), "success");

                                } else {

                                    response(context, 200, new JsonObject()
                                            .put(Constant.STATUS, Constant.FAIL)
                                            .put(Constant.ERROR, "id not found in database"));
                                }
                            } else {

                                response(context, 400, new JsonObject().put(Constant.ERROR,
                                                eventBusHandler.cause().getMessage())
                                        .put(Constant.STATUS, Constant.FAIL));

                            }
                        } catch (Exception exception) {

                            response(context, 500, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                                    .put(Constant.ERROR, exception.getMessage()));
                        }

                    });
        } catch (Exception exception) {

            response(context, 500, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                    .put(Constant.ERROR, exception.getMessage()));
        }
    }

    private void getPollingData(RoutingContext context) {

        LOG.info("metric get polling data fn() called");
        Promise<String> promise = Promise.promise();
        Future<String> future = promise.future();

        try {
            int limitVal = 0;
            String query = "select * from polling where monitor_id= idValue and metric_group= \"groupValue\" order by id desc limit limitValue";

            var groups = context.queryParam("group");

            var limit = context.queryParam("limit");

            if (limit.isEmpty() || Integer.parseInt(limit.get(0)) > 10 || Integer.parseInt(limit.get(0)) < 0) {

                limitVal = 10;

            } else {

                limitVal = Integer.parseInt(context.queryParam("limit").get(0));

            }

            if (!groups.isEmpty()) {

                query = query.replace("idValue", context.pathParam(Constant.ID))
                        .replace("groupValue", groups.get(0))
                        .replace("limitValue", String.valueOf(limitVal)) + ";";

                promise.complete(query);

            } else {
                String finalQuery = query;
                int finalLimitVal = limitVal;

                String getMetricGroup = "select metric_group from metric_values where type =(select type from monitor where monitor_id="
                        + context.pathParam(Constant.ID) + ");";

                eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject()
                        .put(Constant.QUERY, getMetricGroup)
                        .put(Constant.METHOD_TYPE, Constant.GET_DATA), eventBusHandler -> {

                    try {

                        if (eventBusHandler.succeeded()) {

                            var result = eventBusHandler.result().body().getJsonArray(Constant.DATA);

                            if (!result.isEmpty()) {

                                var queryBuilder = new StringBuilder();

                                for (int i = 0; i < result.size(); i++) {

                                    var tempQuery = finalQuery.replace("idValue",
                                                    context.pathParam("id")).replace("groupValue",
                                                    result.getJsonObject(i).getString("metric.group"))
                                            .replace("limitValue", String.valueOf(finalLimitVal));

                                    queryBuilder.append("( ").append(tempQuery).append(") ").append("union");

                                }

                                queryBuilder.setLength(queryBuilder.length() - 5);

                                queryBuilder.append(";");

                                promise.complete(queryBuilder.toString());

                            } else {

                                promise.fail("id not found in database");

                            }

                        } else {

                            promise.fail(eventBusHandler.cause().getMessage());

                        }

                    } catch (Exception exception) {

                        response(context, 500, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                                .put(Constant.ERROR, exception.getCause().getMessage()));
                    }
                });
            }


            future.onComplete(futureCompleteHandler -> {
                try {
                    if (futureCompleteHandler.succeeded()) {
                        eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject()
                                .put("query", futureCompleteHandler.result())
                                .put(Constant.METHOD_TYPE, Constant.GET_DATA), eventBusHandler -> {
                            try{

                            if (eventBusHandler.succeeded()) {

                                var result = eventBusHandler.result().body()
                                        .getJsonArray(Constant.DATA);
                                if (!result.isEmpty()) {

                                    response(context, 200, new JsonObject()
                                            .put(Constant.STATUS, Constant.SUCCESS)
                                            .put(Constant.RESULT, result));
                                } else {

                                    response(context, 200, new JsonObject()
                                            .put(Constant.STATUS, Constant.FAIL)
                                            .put(Constant.ERROR, "id not found in database"));

                                }
                            } else {

                                response(context, 400, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                                        .put(Constant.ERROR, eventBusHandler.cause().getMessage()));

                            }
                        }catch (Exception exception){

                                response(context, 500, new JsonObject()
                                        .put(Constant.STATUS, Constant.FAIL)
                                        .put(Constant.ERROR, exception.getCause().getMessage()));

                            }
                        });
                    } else {

                        response(context, 400, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                                .put(Constant.ERROR, futureCompleteHandler.cause().getMessage()));

                    }
                } catch (Exception exception) {

                    response(context, 500, new JsonObject()
                            .put(Constant.STATUS, Constant.FAIL)
                            .put(Constant.ERROR, exception.getCause().getMessage()));
                }
            });

        } catch (Exception e) {
            response(context, 500, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                    .put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, e.getMessage()));
        }

    }

    private void get(RoutingContext context) {

        LOG.info("metric get all fn() called");

        try {

            var query = "Select * from metric;";

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject().put("query", query)
                    .put(Constant.METHOD_TYPE, Constant.GET_DATA), eventBusHandler -> {

                if (eventBusHandler.succeeded()) {

                    var result = eventBusHandler.result().body().getValue("data");

                    response(context, 200, new JsonObject()
                            .put(Constant.STATUS, Constant.SUCCESS)
                            .put("result", result));

                } else {

                    response(context, 200, new JsonObject()
                            .put(Constant.STATUS, Constant.FAIL)
                            .put(Constant.ERROR, eventBusHandler.cause().getMessage()));
                }
            });
        } catch (Exception e) {

            response(context, 500, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                    .put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, e.getMessage()));

        }

    }

    private void update(RoutingContext context) {

        LOG.info("metric update fn() called");

        String metricTimeUpdateQuery = "update  metric set time=" + context.getBodyAsJson().getInteger("time")
                + " where id=" + context.getBodyAsJson().getInteger("id") + ";";

        eventBus.request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject().put("query", metricTimeUpdateQuery)
                .put(Constant.METHOD_TYPE, Constant.UPDATE_DATA), eventBusHandler -> {

            if (eventBusHandler.succeeded()) {

                response(context, 200, new JsonObject()
                        .put(Constant.STATUS, Constant.SUCCESS)
                        .put(Constant.MESSAGE, "metric time updated successfully"));

            } else {

                response(context, 400, new JsonObject()
                        .put(Constant.STATUS, Constant.FAIL)
                        .put(Constant.ERROR, eventBusHandler.cause().getMessage()));

            }
        });

    }

    private void validate(RoutingContext context) {

        LOG.info("metric validate fn() called");

        try {
            if ((context.request().method() != HttpMethod.DELETE)
                    && (context.request().method() != HttpMethod.GET)) {

                if (context.getBodyAsJson() == null) {

                    response(context, 400, new JsonObject()
                            .put(Constant.STATUS, Constant.FAIL)
                            .put(Constant.MESSAGE, "wrong json format"));

                }

                context.getBodyAsJson().stream().forEach(value -> {

                    if (context.getBodyAsJson().getValue(value.getKey()) instanceof String) {

                        context.getBodyAsJson().put(value.getKey(),
                                context.getBodyAsJson().getString(value.getKey()).trim());

                    }
                });

                context.setBody(context.getBodyAsJson().toBuffer());
            }


            if (Objects.equals(context.request().method().toString(), "PUT")) {

                validateUpdate(context);

            } else if (Objects.equals(context.request().method().toString(), "GET")) {

                if (context.pathParam("id") == null) {

                    response(context, 400, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                            .put(Constant.ERROR, "id is null"));

                    LOG.error("id is null");
                } else {

                    context.next();

                }

            } else {

                response(context, 400, new JsonObject()
                        .put(Constant.STATUS, Constant.FAIL)
                        .put(Constant.ERROR, "wrong request"));
            }

        } catch (Exception exception) {

            response(context, 500, new JsonObject().put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, exception.getMessage()));

        }

    }

    private void validateUpdate(RoutingContext context) {

        LOG.info("validate update fn() called");

        if (context.pathParam(Constant.ID) == null) {
            response(context, 400, new JsonObject()
                    .put(Constant.MESSAGE, "id is null")
                    .put(Constant.STATUS, Constant.FAIL));
        } else {

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS,
                    new JsonObject().put(Constant.ID,
                                    Integer.parseInt(context.pathParam(Constant.ID)))
                            .put(Constant.REQUEST_POINT, Constant.METRIC)
                            .put(Constant.METHOD_TYPE, Constant.CHECK_ID),
                    eventBusHandler -> {
                        if (eventBusHandler.succeeded()
                                && eventBusHandler.result().body() != null) {

                            if (eventBusHandler.result().body().getString(Constant.STATUS)
                                    .equals(Constant.FAIL)) {

                                response(context, 400, new JsonObject().put(Constant.MESSAGE,
                                                eventBusHandler.result().body().getString(Constant.ERROR))
                                        .put(Constant.STATUS, Constant.FAIL));

                            } else {
                                try {
                                    if (!context.getBodyAsJson().containsKey("time")
                                            || !(context.getBodyAsJson().getValue("time") instanceof Integer)
                                            || context.getBodyAsJson().getInteger("time") == null) {

                                        response(context, 400, new JsonObject()
                                                .put(Constant.STATUS, Constant.FAIL)
                                                .put(Constant.ERROR, "time Required"));

                                    } else if ((context.getBodyAsJson().getInteger("time") < 6 * 1000)
                                            || (context.getBodyAsJson().getInteger("time") > 86400 * 1000)
                                            || context.getBodyAsJson().getInteger("time") % 1000 != 0) {

                                        response(context, 400, new JsonObject()
                                                .put(Constant.STATUS, Constant.FAIL)
                                                .put(Constant.ERROR
                                                        , "time out of range or multiple of 1000 ( range[ 6s,86400s])"));

                                    } else {

                                        context.setBody(context.getBodyAsJson().put("id",
                                                        Integer.parseInt(context.pathParam(Constant.ID)))
                                                .toBuffer());

                                        context.next();

                                    }
                                } catch (Exception exception) {

                                    response(context, 500, new JsonObject()
                                            .put(Constant.STATUS, Constant.FAIL)
                                            .put(Constant.MESSAGE, "wrong json format")
                                            .put(Constant.ERROR, exception.getMessage()));
                                }
                            }
                        } else {

                            response(context, 400, new JsonObject()
                                    .put(Constant.STATUS, Constant.FAIL)
                                    .put(Constant.ERROR, eventBusHandler.cause().getMessage()));
                        }
                    });
        }
    }

    private void response(RoutingContext context, int statusCode, JsonObject reply) {

        var httpResponse = context.response();

        httpResponse.setStatusCode(statusCode).putHeader(Constant.HEADER_TYPE, Constant.HEADER_VALUE)
                .end(reply.encodePrettily());

    }
}
