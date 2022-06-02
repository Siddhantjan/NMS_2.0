package com.mindarray.api;

import com.mindarray.Bootstrap;
import com.mindarray.Constant;
import com.mindarray.Utils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class Discovery {
    private static final Logger LOG = LoggerFactory.getLogger(Discovery.class.getName());

    private final EventBus eventBus = Bootstrap.vertx.eventBus();

    public void init(Router router) {
        LOG.info("Discovery Init fn () Called .....");

        router.route().method(HttpMethod.POST).path(Constant.DISCOVERY_POINT)
                .handler(this::validate).handler(this::create);

        router.route().method(HttpMethod.PUT).path(Constant.DISCOVERY_POINT + "/:id/")
                .handler(this::inputValidate).handler(this::validate).handler(this::update);

        router.route().method(HttpMethod.DELETE).path(Constant.DISCOVERY_POINT + "/:id/")
                .handler(this::validate).handler(this::delete);

        router.route().method(HttpMethod.GET).path(Constant.DISCOVERY_POINT).handler(this::get);

        router.route().method(HttpMethod.GET).path(Constant.DISCOVERY_POINT + "/:id/")
                .handler(this::validate).handler(this::getById);

        router.route().method(HttpMethod.POST).path(Constant.DISCOVERY_POINT + "/run" + "/:id/")
                .handler(this::run);
    }

    private void inputValidate(RoutingContext context) {

        LOG.info("discovery Input validate fn() called");
        var contextBody = new JsonObject();

        try {

            if (context.pathParam(Constant.ID) == null) {

                response(context, 400, new JsonObject()
                        .put(Constant.MESSAGE, "id is null")
                        .put(Constant.STATUS, Constant.FAIL));

            } else {

                eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject().put(Constant.ID,
                                        Integer.parseInt(context.pathParam(Constant.ID)))
                                .put(Constant.REQUEST_POINT, Constant.DISCOVERY)
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

                                        var validateInputArray = Utils.inputValidation();

                                        if (context.getBodyAsJson().containsKey(Constant.TYPE)
                                                && ((context.getBodyAsJson().getString(Constant.TYPE).equals("linux"))
                                                || (context.getBodyAsJson().getString(Constant.TYPE).equals("windows"))
                                                || (context.getBodyAsJson().getString(Constant.TYPE).equals("network")))) {

                                            var protocolValue = context.getBodyAsJson().getString(Constant.TYPE);

                                            validateInputArray.get(protocolValue).forEach(keyElement -> {

                                                var key = keyElement.toString();

                                                if (context.getBodyAsJson().containsKey(key)) {

                                                    contextBody.put(key, context.getBodyAsJson().getValue(key));

                                                }
                                            });

                                            contextBody.remove(Constant.TYPE);

                                            context.setBody(contextBody.put(Constant.DISCOVERY_ID,
                                                            Integer.parseInt(context.pathParam(Constant.ID)))
                                                    .toBuffer());

                                            context.next();

                                        } else {

                                            response(context, 400, new JsonObject()
                                                    .put(Constant.STATUS, Constant.FAIL)
                                                    .put(Constant.ERROR, "type required or you entered wrong type"));
                                        }
                                    } catch (Exception e) {

                                        response(context, 500, new JsonObject()
                                                .put(Constant.STATUS, Constant.FAIL)
                                                .put(Constant.MESSAGE, "wrong json format")
                                                .put(Constant.ERROR, e.getMessage()));
                                    }
                                }
                            } else {

                                response(context, 400, new JsonObject()
                                        .put(Constant.STATUS, Constant.FAIL)
                                        .put(Constant.ERROR, eventBusHandler.cause().getMessage()));
                            }
                        });
            }


        } catch (Exception e) {

            response(context, 400, new JsonObject()
                    .put(Constant.STATUS, Constant.FAIL)
                    .put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, e.getMessage()));

        }
    }

    private void run(RoutingContext context) {

        Promise<JsonObject> promise = Promise.promise();
        Promise<JsonObject> promiseDiscovery = Promise.promise();
        Future<JsonObject> future = promise.future();
        Future<JsonObject> futureDiscovery = promiseDiscovery.future();

        if (context.pathParam(Constant.ID) == null) {

            response(context, 400, new JsonObject()
                    .put(Constant.MESSAGE, "id is null")
                    .put(Constant.STATUS, Constant.FAIL));

        } else {
            String runDiscoveryQuery = "select c.username,c.password,c.community,c.version,d.ip,d.type,d.port from discovery as d JOIN credential as c on d.credential_profile=c.credential_id where d.discovery_id="
                    + Integer.parseInt(context.pathParam(Constant.ID)) + ";";

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject()
                            .put(Constant.QUERY, runDiscoveryQuery)
                            .put(Constant.REQUEST_POINT, Constant.DISCOVERY)
                            .put(Constant.METHOD_TYPE, Constant.RUN_CHECK_DATA),
                    eventBusHandler -> {

                        if (eventBusHandler.succeeded()) {

                            var result = eventBusHandler.result().body().getJsonArray(Constant.DATA);

                            if (!result.isEmpty()) {

                                promise.complete(result.getJsonObject(0));

                            } else {

                                promise.fail("id not exists in discovery");

                            }

                        } else {

                            promise.fail(eventBusHandler.cause().getMessage());

                        }
                    });

            future.onComplete(futureCompletedHandler -> {

                if (futureCompletedHandler.succeeded()) {

                    var discoveryData = futureCompletedHandler.result();

                    eventBus.<JsonObject>request(Constant.DISCOVERY_EVENTBUS_ADDRESS,
                            discoveryData, eventBusHandler -> {
                                try {

                                    if (eventBusHandler.succeeded()) {

                                        var discoveryResult = eventBusHandler.result().body();

                                        promiseDiscovery.complete(discoveryResult);

                                    } else {

                                        promiseDiscovery.fail(eventBusHandler.cause().getMessage());

                                    }
                                } catch (Exception exception) {

                                    promiseDiscovery.fail(exception.getCause().getMessage());

                                }
                            });

                } else {

                    promiseDiscovery.fail(futureCompletedHandler.cause().getMessage());

                }
            });

            futureDiscovery.onComplete(futureCompleteHandler -> {
                try {

                    if (futureCompleteHandler.succeeded()) {

                        var discoveryData = futureCompleteHandler.result();

                        String insertDiscoveryData = "update discovery set result =" + "'"
                                + discoveryData.getValue(Constant.RESULT) + "'" + " where discovery_id ="
                                + Integer.parseInt(context.pathParam(Constant.ID)) + ";";

                        eventBus.request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject()
                                .put(Constant.QUERY, insertDiscoveryData)
                                .put(Constant.METHOD_TYPE, Constant.DISCOVERY_RESULT_INSERT), eventBusHandler -> {
                            try {

                                if (eventBusHandler.succeeded() && eventBusHandler.result().body() != null) {

                                    response(context, 200, new JsonObject()
                                            .put(Constant.STATUS, Constant.SUCCESS)
                                            .put(Constant.MESSAGE, "discovery successful"));
                                } else {

                                    response(context, 400, new JsonObject()
                                            .put(Constant.STATUS, Constant.FAIL)
                                            .put(Constant.ERROR, eventBusHandler.cause().getMessage()));
                                }
                            } catch (Exception exception) {

                                response(context, 500, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                                        .put(Constant.ERROR, exception.getCause().getMessage()));
                            }
                        });
                    } else {

                        response(context, 400, new JsonObject()
                                .put(Constant.STATUS, Constant.FAIL)
                                .put(Constant.ERROR, futureCompleteHandler.cause().getMessage()));
                    }
                } catch (Exception exception) {

                    response(context, 500, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                            .put(Constant.ERROR, exception.getCause().getMessage()));
                }
            });
        }
    }

    private void get(RoutingContext context) {
        LOG.info("discovery get all fn() called ...");

        try {

            var query = "Select * from discovery;";

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject().put(Constant.QUERY, query)
                            .put(Constant.METHOD_TYPE, Constant.GET_DATA),
                    eventBusHandler -> {
                        try {

                            if (eventBusHandler.succeeded()) {

                                var result = eventBusHandler.result().body().getValue(Constant.DATA);

                                response(context, 200, new JsonObject()
                                        .put(Constant.STATUS, Constant.SUCCESS)
                                        .put(Constant.RESULT, result));

                            } else {

                                response(context, 400, new JsonObject()
                                        .put(Constant.STATUS, Constant.FAIL)
                                        .put(Constant.ERROR, eventBusHandler.cause().getMessage()));

                            }
                        } catch (Exception exception) {

                            response(context, 500, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                                    .put(Constant.ERROR, exception.getCause().getMessage()));
                        }

                    });
        } catch (Exception e) {

            response(context, 400, new JsonObject().put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, e.getMessage()));

        }
    }

    private void getById(RoutingContext context) {

        LOG.info("discovery get by id fn() called ");

        try {

            var query = "Select * from discovery where discovery_id ="
                    + context.pathParam(Constant.ID) + ";";

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS,
                    new JsonObject().put(Constant.QUERY, query)
                            .put(Constant.METHOD_TYPE, Constant.GET_DATA)
                            .put(Constant.REQUEST_POINT, Constant.DISCOVERY), eventBusHandler -> {
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

                            response(context, 500, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                                    .put(Constant.ERROR, exception.getCause().getMessage()));
                        }

                    });

        } catch (Exception e) {
            response(context, 400, new JsonObject().put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, e.getMessage()));
        }
    }

    private void delete(RoutingContext context) {

        LOG.info("discovery delete fn() called ");

        try {

            var query = "delete from discovery where discovery_id =" + context.pathParam(Constant.ID) + ";";

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject().put(Constant.QUERY, query)
                    .put(Constant.METHOD_TYPE, Constant.DELETE_DATA), eventBusHandler -> {
                try {

                    if (eventBusHandler.succeeded()) {

                        var deleteResult = eventBusHandler.result().body();

                        if (deleteResult.getString(Constant.STATUS).equals(Constant.SUCCESS)) {

                            response(context, 200, new JsonObject()
                                    .put(Constant.MESSAGE, "id " + context.pathParam(Constant.ID) + " deleted")
                                    .put(Constant.STATUS, Constant.SUCCESS));

                        } else {

                            response(context, 200, new JsonObject()
                                    .put(Constant.ERROR, eventBusHandler.result().body())
                                    .put(Constant.STATUS, Constant.FAIL));
                        }

                    } else {

                        response(context, 400, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                                .put(Constant.ERROR, eventBusHandler.cause().getMessage()));
                    }
                } catch (Exception exception) {

                    response(context, 500, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                            .put(Constant.ERROR, exception.getCause().getMessage()));
                }
            });

        } catch (Exception exception) {
            response(context, 400, new JsonObject().put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, exception.getMessage()));
        }
    }

    private void update(RoutingContext context) {

        LOG.info("discovery update fn() called ");

        try {

            var userData = context.getBodyAsJson();
            var id = userData.getInteger(Constant.DISCOVERY_ID);
            var queryStatement = new StringBuilder();

            userData.forEach(value -> {

                var key = value.getKey().replace(".", "_");
                queryStatement.append(key).append(" = ").append("'")
                        .append(value.getValue()).append("'").append(",");

            });

            var query = "update discovery set "
                    + queryStatement.substring(0, queryStatement.length() - 1)
                    + " where discovery_id = " + id + ";";

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject().put(Constant.QUERY, query)
                    .put(Constant.METHOD_TYPE, Constant.CREATE_DATA)
                    .put(Constant.REQUEST_POINT, Constant.DISCOVERY), eventBusHandler -> {

                try {

                    var result = eventBusHandler.result().body();

                    if (eventBusHandler.succeeded() || result != null) {

                        response(context, 200, new JsonObject()
                                .put(Constant.STATUS, Constant.SUCCESS)
                                .put(Constant.MESSAGE, "id " + id + " updated"));

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

        } catch (Exception exception) {

            response(context, 400, new JsonObject()
                    .put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, exception.getMessage()));

        }
    }

    private void create(RoutingContext context) {
        LOG.info("discovery create fn() called ");
        try {
            var userData = context.getBodyAsJson();

            String query = "insert into discovery(discovery_name,ip,type,credential_profile,port)values('"
                    + userData.getString(Constant.DISCOVERY_NAME).trim() + "','"
                    + userData.getString(Constant.IP).trim() + "','"
                    + userData.getString(Constant.TYPE).trim() + "','"
                    + userData.getInteger(Constant.CREDENTIAL_PROFILE)
                    + "','" + userData.getInteger(Constant.PORT) + "');";

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject().put(Constant.QUERY, query)
                    .put(Constant.METHOD_TYPE, Constant.CREATE_DATA)
                    .put(Constant.REQUEST_POINT, Constant.DISCOVERY), eventBusHandler -> {
                try {
                    if (eventBusHandler.succeeded()) {
                        var handlerResult = eventBusHandler.result().body().getJsonArray(Constant.DATA);
                        var id = handlerResult.getJsonObject(0).getInteger(Constant.ID);

                        response(context, 200, new JsonObject()
                                .put(Constant.MESSAGE, "discovery created successfully")
                                .put(Constant.STATUS, Constant.SUCCESS).put(Constant.ID, id));

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
        } catch (Exception e) {

            response(context, 400, new JsonObject().put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, e.getMessage()));


        }

    }

    private void validate(RoutingContext context) {

        LOG.info("Discovery Validate fn() called...");

        try {

            if ((context.request().method() != HttpMethod.DELETE)
                    && (context.request().method() != HttpMethod.GET)) {

                if (context.getBodyAsJson() == null) {

                    response(context, 400, new JsonObject()
                            .put(Constant.STATUS, Constant.FAIL)
                            .put(Constant.MESSAGE, "wrong json format"));

                }

                var credentials = context.getBodyAsJson();

                credentials.forEach(value -> {

                    if (credentials.getValue(value.getKey()) instanceof String) {

                        credentials.put(value.getKey(), credentials.getString(value.getKey()).trim());

                    }

                });

                context.setBody(credentials.toBuffer());

            }

            switch (context.request().method().toString()) {

                case "POST" -> validateCreate(context);

                case "PUT" -> validateUpdate(context);

                case "DELETE", "GET" -> {

                    if (context.pathParam(Constant.ID) == null) {

                        response(context, 400, new JsonObject()
                                .put(Constant.MESSAGE, "id is null")
                                .put(Constant.STATUS, Constant.FAIL));

                    } else {

                        eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject().put(Constant.ID,
                                                Integer.parseInt(context.pathParam(Constant.ID)))
                                        .put(Constant.REQUEST_POINT, Constant.DISCOVERY)
                                        .put(Constant.METHOD_TYPE, Constant.CHECK_ID),
                                eventBusHandler -> {
                                    try {

                                        if (eventBusHandler.succeeded()
                                                && eventBusHandler.result().body() != null) {

                                            if (eventBusHandler.result().body().getString(Constant.STATUS)
                                                    .equals(Constant.FAIL)) {

                                                response(context, 400, new JsonObject().put(Constant.MESSAGE,
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
                                    } catch (Exception exception) {

                                        response(context, 500, new JsonObject()
                                                .put(Constant.STATUS, Constant.FAIL)
                                                .put(Constant.ERROR, exception.getCause().getMessage()));

                                    }

                                });
                    }
                }

                default -> response(context, 400, new JsonObject()
                        .put(Constant.STATUS, Constant.FAIL)
                        .put(Constant.MESSAGE, "wrong method request"));
            }


        } catch (Exception exception) {
            response(context, 500, new JsonObject().put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, exception.getMessage()));

        }

    }

    private void validateUpdate(RoutingContext context) {

        var errors = new ArrayList<String>();

        if (!context.getBodyAsJson().containsKey(Constant.DISCOVERY_ID)
                || !(context.getBodyAsJson().getValue(Constant.DISCOVERY_ID) instanceof Integer)) {

            errors.add("discovery.id is required");

        }
        if (context.getBodyAsJson().containsKey(Constant.DISCOVERY_NAME)
                && (!(context.getBodyAsJson().getValue(Constant.DISCOVERY_NAME) instanceof String)
                || context.getBodyAsJson().getString(Constant.DISCOVERY_NAME).isEmpty())){
            errors.add("discovery.name is required");

        }
        if (context.getBodyAsJson().containsKey(Constant.IP)
                &&( !(context.getBodyAsJson().getValue(Constant.IP) instanceof String)
                || context.getBodyAsJson().getString(Constant.IP).isEmpty())){
            errors.add("IP is required");
        }
        if (context.getBodyAsJson().containsKey(Constant.CREDENTIAL_PROFILE)
                &&( !(context.getBodyAsJson().getValue(Constant.CREDENTIAL_PROFILE) instanceof Integer)
                || context.getBodyAsJson().getString(Constant.CREDENTIAL_PROFILE).isEmpty())){
            errors.add("credential.profile is required");
        }
        if (context.getBodyAsJson().containsKey(Constant.PORT)
                &&( !(context.getBodyAsJson().getValue(Constant.PORT) instanceof Integer)
                || context.getBodyAsJson().getString(Constant.PORT).isEmpty())){
            errors.add("port is required");
        }

            if (errors.isEmpty()) {

                eventBus.request(Constant.DATABASE_EVENTBUS_ADDRESS, context.getBodyAsJson()
                        .put(Constant.METHOD_TYPE, Constant.CHECK_DISCOVERY_UPDATES)
                        .put(Constant.REQUEST_POINT, Constant.DISCOVERY), eventBusHandler -> {
                    try {

                        if (eventBusHandler.succeeded()) {

                            context.next();

                        } else {

                            response(context, 400, new JsonObject().put(Constant.STATUS, Constant.FAIL)
                                    .put(Constant.ERROR, eventBusHandler.cause().getMessage()));

                        }
                    } catch (Exception exception) {

                        response(context, 500, new JsonObject()
                                .put(Constant.STATUS, Constant.FAIL)
                                .put(Constant.ERROR, exception.getCause().getMessage()));

                    }
                });

            } else {

                response(context, 400, new JsonObject().put(Constant.ERROR, errors)
                        .put(Constant.STATUS, Constant.FAIL));

            }
    }

    private void validateCreate(RoutingContext context) {

        var errors = new ArrayList<String>();

        if (!context.getBodyAsJson().containsKey(Constant.DISCOVERY_NAME)
                || !(context.getBodyAsJson().getValue(Constant.DISCOVERY_NAME) instanceof String)
                || context.getBodyAsJson().getString(Constant.DISCOVERY_NAME).isEmpty()) {

            errors.add("discovery.name is required");
            LOG.error("discovery.name is required");

        }

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

        if (!context.getBodyAsJson().containsKey(Constant.CREDENTIAL_PROFILE)
                || !(context.getBodyAsJson().getValue(Constant.CREDENTIAL_PROFILE) instanceof Integer)
                || context.getBodyAsJson().getInteger(Constant.CREDENTIAL_PROFILE) == null) {

            errors.add("credential.profile is required (int)");
            LOG.error("credential.profile is required");

        }

        if (!context.getBodyAsJson().containsKey(Constant.PORT)
                || !(context.getBodyAsJson().getValue(Constant.PORT) instanceof Integer)
                || context.getBodyAsJson().getInteger(Constant.PORT) == null) {

            errors.add("port is required (int)");
            LOG.error("port is required");

        }

        if (errors.isEmpty()) {

            eventBus.request(Constant.DATABASE_EVENTBUS_ADDRESS, context.getBodyAsJson()
                    .put(Constant.METHOD_TYPE, Constant.CHECK_MULTI_FIELDS)
                    .put(Constant.REQUEST_POINT, Constant.DISCOVERY), eventBusHandler -> {
                try {

                    if (eventBusHandler.succeeded()) {

                        context.next();

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
        } else {

            response(context, 400, new JsonObject()
                    .put(Constant.ERROR, errors).put(Constant.STATUS, Constant.FAIL));

        }
    }

    private void response(RoutingContext context, int statusCode, JsonObject reply) {

        var httpResponse = context.response();

        httpResponse.setStatusCode(statusCode)
                .putHeader(Constant.HEADER_TYPE, Constant.HEADER_VALUE)
                .end(reply.encodePrettily());

    }
}
