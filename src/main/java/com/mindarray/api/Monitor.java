package com.mindarray.api;

import com.mindarray.Bootstrap;
import com.mindarray.Constant;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class Monitor {
    private static final Logger LOG = LoggerFactory.getLogger(Monitor.class.getName());
    private final EventBus eventBus = Bootstrap.vertx.eventBus();

    public void init(Router router) {
        LOG.info("start -> {}", " Monitor Init Called .....");

        router.route().method(HttpMethod.POST).path(Constant.MONITOR_POINT +
                Constant.PROVISION_POINT).handler(this::validate).handler(this::create);

        router.route().method(HttpMethod.DELETE).path(Constant.MONITOR_POINT + "/:id/")
                .handler(this::validate).handler(this::delete);

        router.route().method(HttpMethod.GET).path(Constant.MONITOR_POINT).handler(this::get);

        router.route().method(HttpMethod.GET).path(Constant.MONITOR_POINT + "/:id/")
                .handler(this::validate).handler(this::getById);

        router.route().method(HttpMethod.PUT).path(Constant.MONITOR_POINT + "/:id/")
                .handler(this::validate).handler(this::update);
    }

    private void update(RoutingContext context) {

        String portUpdateQuery = "update  monitor set port=" + context.getBodyAsJson().getInteger(Constant.PORT)
                + " where monitor_id=" + context.getBodyAsJson().getInteger(Constant.MONITOR_ID) + ";";

        eventBus.request(Constant.DATABASE_EVENTBUS_ADDRESS,
                new JsonObject().put(Constant.QUERY, portUpdateQuery)
                        .put(Constant.METHOD_TYPE, Constant.UPDATE_DATA), eventBusHandler -> {

                    try {

                        if (eventBusHandler.succeeded()) {

                            response(context, 200, new JsonObject()
                                    .put(Constant.STATUS, Constant.SUCCESS)
                                    .put(Constant.MESSAGE, "monitor updated successfully"));

                        } else {

                            response(context, 400, new JsonObject()
                                    .put(Constant.STATUS, Constant.FAIL)
                                    .put(Constant.ERROR, eventBusHandler.cause().getMessage()));

                        }

                    } catch (Exception exception) {

                        response(context, 500, new JsonObject()
                                .put(Constant.STATUS, Constant.FAIL)
                                .put(Constant.ERROR, exception.getCause().getMessage()));
                    }
                });
    }

    private void getById(RoutingContext context) {

        LOG.info("monitor get by id fn() called");
        try {

            var query = "Select * from monitor where monitor_id ="
                    + context.pathParam(Constant.ID) + ";";

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject()
                            .put(Constant.QUERY, query).put(Constant.METHOD_TYPE, Constant.GET_DATA),
                    eventBusHandler -> {
                        try {
                            if (eventBusHandler.succeeded()) {

                                var result = eventBusHandler.result().body().getValue(Constant.DATA);

                                response(context, 200, new JsonObject()
                                        .put(Constant.STATUS, Constant.SUCCESS)
                                        .put(Constant.RESULT, result));

                            } else {

                                response(context, 200, new JsonObject()
                                        .put(Constant.STATUS, Constant.FAIL)
                                        .put(Constant.ERROR, eventBusHandler.cause().getMessage()));
                            }
                        } catch (Exception exception) {

                            response(context, 500, new JsonObject()
                                    .put(Constant.STATUS, Constant.FAIL)
                                    .put(Constant.ERROR, exception.getCause().getMessage()));
                        }
                    });
        } catch (Exception exception) {

            response(context, 400, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                    .put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, exception.getMessage()));
        }
    }

    private void get(RoutingContext context) {
        LOG.info("monitor get all fn() called");

        try {

            var query = "Select * from monitor;";

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS,

                    new JsonObject().put(Constant.QUERY, query)
                            .put(Constant.METHOD_TYPE, Constant.GET_DATA), eventBusHandler -> {

                        try {

                            if (eventBusHandler.succeeded()) {

                                var result = eventBusHandler.result().body().getValue(Constant.DATA);

                                response(context, 200, new JsonObject()
                                        .put(Constant.STATUS, Constant.SUCCESS)
                                        .put(Constant.RESULT, result));

                            } else {

                                response(context, 200,
                                        new JsonObject().put(Constant.STATUS, Constant.FAIL)
                                                .put(Constant.ERROR, eventBusHandler.cause().getMessage()));
                            }

                        } catch (Exception exception) {

                            response(context, 500, new JsonObject()
                                    .put(Constant.STATUS, Constant.FAIL)
                                    .put(Constant.ERROR, exception.getCause().getMessage()));
                        }
                    });

        } catch (Exception exception) {

            response(context, 500, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                    .put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, exception.getMessage()));
        }

    }

    private void delete(RoutingContext context) {

        LOG.info("monitor delete fn() called");

        try {
            Promise<JsonObject> promise = Promise.promise();

            Future<JsonObject> future = promise.future();

            String getMetricIDQuery = "select id from metric where monitor_id="
                    + context.pathParam(Constant.ID) + ";";

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject()
                    .put(Constant.QUERY, getMetricIDQuery)
                    .put(Constant.METHOD_TYPE, Constant.GET_DATA), eventBusHandler -> {

                try {

                    if (eventBusHandler.succeeded()) {

                        promise.complete(eventBusHandler.result().body());

                    } else {

                        promise.fail(eventBusHandler.cause().getMessage());

                    }
                } catch (Exception exception) {

                    promise.fail(exception.getCause().getMessage());

                }
            });

            future.onComplete(futureCompleteHandler -> {

                if (futureCompleteHandler.succeeded()) {

                    var query = "delete from monitor where monitor_id ="
                            + context.pathParam(Constant.ID) + ";";

                    eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS,
                            new JsonObject().put(Constant.QUERY, query)
                                    .put(Constant.METHOD_TYPE, Constant.MONITOR_DELETE)
                                    .put(Constant.REQUEST_POINT, Constant.MONITOR)
                                    .put(Constant.MONITOR_ID, context.pathParam(Constant.ID)),
                            eventBusHandler -> {
                                try {

                                    var deleteResult = eventBusHandler.result().body();

                                    if (deleteResult.getString(Constant.STATUS).equals(Constant.SUCCESS)) {

                                        response(context, 200, new JsonObject()
                                                .put(Constant.MESSAGE, "id " + context.pathParam(Constant.ID)
                                                        + " deleted")
                                                .put(Constant.STATUS, Constant.SUCCESS));

                                        eventBus.send(Constant.METRIC_SCHEDULER_EVENTBUS_DELETE_ADDRESS
                                                , futureCompleteHandler.result());

                                    } else {

                                        response(context, 400, new JsonObject()
                                                .put(Constant.ERROR, eventBusHandler.result().body())
                                                .put(Constant.STATUS, Constant.FAIL));
                                    }
                                } catch (Exception exception) {

                                    response(context, 500, new JsonObject()
                                            .put(Constant.STATUS, Constant.FAIL)
                                            .put(Constant.ERROR, exception.getCause().getMessage()));
                                }

                            });

                } else {
                    response(context, 400, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                            .put(Constant.ERROR, futureCompleteHandler.cause().getMessage()));
                }
            });
        } catch (Exception e) {

            response(context, 400, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                    .put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, e.getMessage()));

        }
    }

    private void create(RoutingContext context) {

        LOG.info("monitor create fn() called");

        var monitorData = context.getBodyAsJson();

        String query = "insert into monitor (ip,host,type,port) values("
                + "'" + monitorData.getString(Constant.IP) + "','"
                + monitorData.getString(Constant.HOST) + "','"
                + monitorData.getString(Constant.TYPE) + "',"
                + monitorData.getInteger(Constant.PORT) + ");";

        eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject().put(Constant.QUERY, query)
                .put(Constant.METHOD_TYPE, Constant.CREATE_DATA)
                .put(Constant.REQUEST_POINT, Constant.MONITOR), eventBusHandler -> {

            try {

                if (eventBusHandler.succeeded()) {

                    var result = eventBusHandler.result().body().getJsonArray(Constant.DATA);
                    var id = result.getJsonObject(0).getInteger(Constant.ID);

                    response(context, 200, new JsonObject()
                            .put(Constant.STATUS, Constant.SUCCESS)
                            .put(Constant.MESSAGE, "monitor created successfully")
                            .put(Constant.ID, id));

                    metricGroupCreate(context.getBodyAsJson().put(Constant.MONITOR_ID, id));

                } else {
                    response(context, 400, new JsonObject()
                            .put(Constant.STATUS, Constant.FAIL)
                            .put(Constant.ERROR, eventBusHandler.cause().getMessage()));

                }

            } catch (Exception exception) {

                response(context, 500, new JsonObject()
                        .put(Constant.STATUS, Constant.FAIL)
                        .put(Constant.ERROR, exception.getCause().getMessage()));
            }

        });

    }

    private void metricGroupCreate(JsonObject monitorData) {

        LOG.info("metric group create fn() called");

        Promise<JsonObject> promiseAddMetric = Promise.promise();
        Future<JsonObject> futureAddMetric = promiseAddMetric.future();

        StringBuilder values = new StringBuilder();

        String query = "select metric_group,time,monitor_id from metric_values,monitor where metric_values.type=monitor.type and monitor_id="
                + monitorData.getInteger(Constant.MONITOR_ID) + ";";

        eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject()
                        .put(Constant.QUERY, query)
                        .put(Constant.METHOD_TYPE, Constant.GET_DATA),
                eventBusHandler -> {
                    try {

                        if (eventBusHandler.succeeded()) {
                            var resultJsonArrayData = eventBusHandler.result().body()
                                    .getJsonArray(Constant.DATA);

                            for (int index = 0; index < resultJsonArrayData.size(); index++) {

                                if (!monitorData.getString(Constant.TYPE).equals(Constant.NETWORK)) {

                                    resultJsonArrayData.getJsonObject(index).mergeIn(new JsonObject()
                                            .put(Constant.CREDENTIAL_PROFILE,
                                                    monitorData.getInteger(Constant.CREDENTIAL_ID)));

                                } else {

                                    var getObjects = monitorData.getJsonObject("objects")
                                            .getJsonArray("interfaces");

                                    var upObjects = new JsonArray();

                                    for (int objectIndex = 0; objectIndex <= getObjects.size() - 1; objectIndex++) {

                                        var value = getObjects.getJsonObject(objectIndex);

                                        if (value.getString("interface.operational.status").equals("up")) {

                                            upObjects.add(value);

                                        }
                                    }

                                    resultJsonArrayData.getJsonObject(index).mergeIn(new JsonObject()
                                            .put(Constant.CREDENTIAL_PROFILE,
                                                    monitorData.getInteger(Constant.CREDENTIAL_ID))
                                            .put("objects", new JsonObject().put("interfaces", upObjects)));
                                }
                            }

                            if (!resultJsonArrayData.isEmpty()) {

                                String addMetricQuery = "";

                                for (int metricDataIndex = 0;
                                     metricDataIndex < resultJsonArrayData.size();
                                     metricDataIndex++) {

                                    var value = resultJsonArrayData.getJsonObject(metricDataIndex);

                                    if (monitorData.getString(Constant.TYPE).equals(Constant.NETWORK)) {

                                        values.append("(")
                                                .append(value.getInteger(Constant.MONITOR_ID))
                                                .append(",").append(monitorData.getInteger(Constant.CREDENTIAL_ID))
                                                .append(",'").append(value.getString(Constant.METRIC_GROUP))
                                                .append("',").append(value.getInteger("time"))
                                                .append(",'").append(value.getJsonObject("objects"))
                                                .append("'").append("),");

                                    } else {

                                        values.append("(")
                                                .append(value.getInteger(Constant.MONITOR_ID))
                                                .append(",").append(monitorData.getInteger(Constant.CREDENTIAL_ID))
                                                .append(",'").append(value.getString(Constant.METRIC_GROUP))
                                                .append("',").append(value.getInteger("time")).append("),");
                                    }
                                }

                                if (monitorData.getString(Constant.TYPE).equals(Constant.NETWORK)) {

                                    addMetricQuery = "insert into metric(monitor_id,credential_profile,metric_group,time,objects)values"
                                            + values.substring(0, values.toString().length() - 1) + ";";

                                    promiseAddMetric.complete(new JsonObject()
                                            .put(Constant.QUERY, addMetricQuery)
                                            .put(Constant.METHOD_TYPE, Constant.METRIC_CREATE));
                                } else {

                                    addMetricQuery = "insert into metric(monitor_id,credential_profile,metric_group,time)values"
                                            + values.substring(0, values.toString().length() - 1) + ";";

                                    promiseAddMetric.complete(new JsonObject()
                                            .put(Constant.QUERY, addMetricQuery)
                                            .put(Constant.METHOD_TYPE, Constant.METRIC_CREATE));
                                }

                            } else {

                                promiseAddMetric.fail("result Json array data is empty");

                            }
                        } else {

                            LOG.error(eventBusHandler.cause().getMessage());

                        }
                    } catch (Exception exception) {

                        LOG.warn("EXCEPTION->{}", exception.getCause().getMessage());

                    }
                });

        futureAddMetric.onComplete(futureCompleteHandler -> {
            if (futureCompleteHandler.succeeded()) {

                eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS,
                        futureCompleteHandler.result(), eventBusHandler -> {

                            if (eventBusHandler.succeeded()) {
                                LOG.info("metrics added");

                                eventBus.send(Constant.METRIC_SCHEDULER_EVENTBUS_ADDRESS,
                                        monitorData.getInteger(Constant.CREDENTIAL_ID));

                            } else {

                                LOG.error(eventBusHandler.cause().getMessage());
                            }

                        });
            } else {

                LOG.error(futureCompleteHandler.cause().getMessage());

            }
        });

    }

    private void validate(RoutingContext context) {

        LOG.info(" monitor validate fn() called");

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

            switch (context.request().method().toString()) {
                case "POST" -> validateCreate(context);

                case "PUT" -> validateUpdate(context);

                case "DELETE", "GET" -> {

                    if (context.pathParam(Constant.ID) == null) {

                        response(context, 400, new JsonObject().put(Constant.MESSAGE, "id is null")
                                .put(Constant.STATUS, Constant.FAIL));

                    } else {

                        eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS,
                                new JsonObject()
                                        .put(Constant.ID, Integer.parseInt(context.pathParam(Constant.ID)))
                                        .put(Constant.REQUEST_POINT, Constant.MONITOR)
                                        .put(Constant.METHOD_TYPE, Constant.CHECK_ID), eventBusHandler -> {

                                    if (eventBusHandler.succeeded() && eventBusHandler.result().body() != null) {

                                        if (eventBusHandler.result().body().getString(Constant.STATUS)
                                                .equals(Constant.FAIL)) {

                                            response(context, 400, new JsonObject()
                                                    .put(Constant.ERROR,
                                                            eventBusHandler.result().body().getString(Constant.ERROR))
                                                    .put(Constant.STATUS, Constant.FAIL));

                                        } else {

                                            context.next();

                                        }
                                    } else {

                                        response(context, 400, new JsonObject()
                                                .put(Constant.STATUS, Constant.FAIL)
                                                .put(Constant.ERROR, eventBusHandler.cause().getMessage()));

                                    }
                                });
                    }
                }
                default -> response(context, 400, new JsonObject()
                        .put(Constant.STATUS, Constant.FAIL)
                        .put(Constant.MESSAGE, "wrong method"));
            }
        } catch (Exception exception) {

            response(context, 500, new JsonObject().put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, exception.getMessage()));
        }


    }

    private void validateUpdate(RoutingContext context) {

        if (context.pathParam(Constant.ID) == null) {

            response(context, 400, new JsonObject()
                    .put(Constant.MESSAGE, "id is null")
                    .put(Constant.STATUS, Constant.FAIL));

        } else {

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject().put(Constant.ID,
                                    Integer.parseInt(context.pathParam(Constant.ID)))
                            .put(Constant.REQUEST_POINT, Constant.MONITOR)
                            .put(Constant.METHOD_TYPE, Constant.CHECK_ID),
                    eventBusHandler -> {


                        if (eventBusHandler.succeeded() && eventBusHandler.result().body() != null) {

                            if (eventBusHandler.result().body().getString(Constant.STATUS)
                                    .equals(Constant.FAIL)) {

                                response(context, 400, new JsonObject().put(Constant.MESSAGE,
                                                eventBusHandler.result().body().getString(Constant.ERROR))
                                        .put(Constant.STATUS, Constant.FAIL));

                            } else {
                                try {

                                    if (!context.getBodyAsJson().containsKey(Constant.PORT)
                                            || !(context.getBodyAsJson().getValue(Constant.PORT) instanceof Integer)
                                            || context.getBodyAsJson().getInteger(Constant.PORT) == null) {

                                        response(context, 400, new JsonObject()
                                                .put(Constant.STATUS, Constant.FAIL)
                                                .put(Constant.ERROR, "port is required or wrong type entered "));

                                    } else {

                                        context.setBody(context.getBodyAsJson().put(Constant.MONITOR_ID,
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

    private void validateCreate(RoutingContext context) {

        LOG.info("monitor validate create fn () called");

        var errors = new ArrayList<String>();

        if (!context.getBodyAsJson().containsKey(Constant.IP)
                || !(context.getBodyAsJson().getValue(Constant.IP) instanceof String)
                || context.getBodyAsJson().getString(Constant.IP).isEmpty()) {

            errors.add("ip is required");
            LOG.error("ip is required");

        }

        if (!context.getBodyAsJson().containsKey(Constant.TYPE)
                || !(context.getBodyAsJson().getValue(Constant.TYPE) instanceof String)
                || context.getBodyAsJson().getString(Constant.TYPE).isEmpty()) {

            errors.add("type is required");
            LOG.error("type is required");

        }

        if (!context.getBodyAsJson().containsKey(Constant.CREDENTIAL_ID)
                || !(context.getBodyAsJson().getValue(Constant.CREDENTIAL_ID) instanceof Integer)
                || context.getBodyAsJson().getInteger(Constant.CREDENTIAL_ID) == null) {

            errors.add("credential.id is required (int)");
            LOG.error("credential.id is required");

        }
        if (!context.getBodyAsJson().containsKey(Constant.PORT)
                || !(context.getBodyAsJson().getValue(Constant.PORT) instanceof Integer)
                || context.getBodyAsJson().getInteger(Constant.PORT) == null) {

            errors.add("port is required (int)");
            LOG.error("port is required");

        }
        if (!context.getBodyAsJson().containsKey(Constant.HOST)
                || !(context.getBodyAsJson().getValue(Constant.HOST) instanceof String)
                || context.getBodyAsJson().getString(Constant.HOST).isEmpty()) {

            errors.add("hostname required");
            LOG.error("hostname required");

        }

        if (context.getBodyAsJson().containsKey(Constant.TYPE)
                && context.getBodyAsJson().getString(Constant.TYPE).equals(Constant.NETWORK)
                && (!context.getBodyAsJson().containsKey("objects")
                || context.getBodyAsJson().isEmpty())) {

            errors.add("objects are required for snmp devices");

            LOG.error("objects are required for snmp devices");

        }

        if (errors.isEmpty()) {

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, context.getBodyAsJson()
                    .put(Constant.REQUEST_POINT, Constant.MONITOR)
                    .put(Constant.METHOD_TYPE, Constant.MONITOR_CHECK), eventBusHandler -> {

                try {

                    if (eventBusHandler.succeeded()) {

                        var responseData = eventBusHandler.result().body();

                        if (responseData.getString(Constant.STATUS).equals(Constant.SUCCESS)) {

                            context.next();

                        } else {

                            response(context, 400, new JsonObject()
                                    .put(Constant.ERROR, responseData.getString(Constant.ERROR))
                                    .put(Constant.STATUS, Constant.FAIL));
                        }

                    } else {

                        response(context, 400, new JsonObject()
                                .put(Constant.ERROR, eventBusHandler.cause().getMessage())
                                .put(Constant.STATUS, Constant.FAIL));

                    }
                } catch (Exception exception) {

                    response(context, 500, new JsonObject()
                            .put(Constant.ERROR, exception.getCause().getMessage())
                            .put(Constant.STATUS, Constant.FAIL));
                }
            });
        } else {

            response(context, 400, new JsonObject().put(Constant.ERROR, errors)
                    .put(Constant.STATUS, Constant.FAIL));
        }


    }

    private void response(RoutingContext context, int statusCode, JsonObject reply) {

        var httpResponse = context.response();

        httpResponse.setStatusCode(statusCode).putHeader(Constant.HEADER_TYPE, Constant.HEADER_VALUE)
                .end(reply.encodePrettily());

    }
}
