package com.mindarray.api;

import com.mindarray.Bootstrap;
import com.mindarray.Constant;
import com.mindarray.Utils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class Credential {
    private static final Logger LOG = LoggerFactory.getLogger(Credential.class.getName());
    private final EventBus eventBus = Bootstrap.vertx.eventBus();

    public void init(Router router) {

        LOG.info("Credential init () called...");

        router.route().method(HttpMethod.POST).path(Constant.CREDENTIAL_POINT)
                .handler(this::validate).handler(this::create);

        router.route().method(HttpMethod.PUT).path(Constant.CREDENTIAL_POINT + "/:id/")
                .handler(this::inputValidate).handler(this::validate).handler(this::update);

        router.route().method(HttpMethod.DELETE).path(Constant.CREDENTIAL_POINT + "/:id/")
                .handler(this::validate).handler(this::delete);

        router.route().method(HttpMethod.GET).path(Constant.CREDENTIAL_POINT).handler(this::get);

        router.route().method(HttpMethod.GET).path(Constant.CREDENTIAL_POINT + "/:id/")
                .handler(this::validate).handler(this::getById);

    }

    private void inputValidate(RoutingContext context) {


        LOG.info("remove unwanted parameters fn() called");

        try {
            if (context.pathParam(Constant.ID) == null) {

                response(context, 400, new JsonObject().put(Constant.MESSAGE, "id is null")
                        .put(Constant.STATUS, Constant.FAIL));
            } else {

                eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject()
                        .put(Constant.ID, Integer.parseInt(context.pathParam(Constant.ID)))
                        .put(Constant.REQUEST_POINT, Constant.CREDENTIAL)
                        .put(Constant.METHOD_TYPE, Constant.CHECK_ID), eventBusHandler -> {

                    try {

                        if (eventBusHandler.succeeded() && eventBusHandler.result().body() != null) {

                            if (eventBusHandler.result().body().getString(Constant.STATUS).equals(Constant.FAIL)) {

                                response(context, 400, new JsonObject().put(Constant.MESSAGE,
                                                eventBusHandler.result().body().getString(Constant.ERROR))
                                        .put(Constant.STATUS, Constant.FAIL));

                            } else {
                                var contextData = context.getBodyAsJson();

                                var validateInputArray = Utils.inputValidation();

                                if (contextData.containsKey(Constant.PROTOCOL)
                                        && ((contextData.getString(Constant.PROTOCOL)
                                        .equals(Constant.SNMP))
                                        || (contextData.getString(Constant.PROTOCOL)
                                        .equals(Constant.POWERSHELL))
                                        || (contextData.getString(Constant.PROTOCOL)
                                        .equals(Constant.SSH)))) {

                                    var protocolValue = contextData.getString(Constant.PROTOCOL);

                                    var keys = contextData.fieldNames();
                                    keys.removeIf(key -> !validateInputArray.get(protocolValue).contains(key));
                                    contextData.remove(Constant.PROTOCOL);
                                    context.setBody(contextData.put(Constant.CREDENTIAL_ID,
                                            Integer.parseInt(context.pathParam(Constant.ID))).toBuffer());
                                    context.next();

                                } else {

                                    response(context, 400, new JsonObject()
                                            .put(Constant.STATUS, Constant.FAIL)
                                            .put(Constant.ERROR, "protocol required or you entered wrong protocol"));

                                }

                            }
                        } else {

                            response(context, 400, new JsonObject()
                                    .put(Constant.STATUS, Constant.FAIL)
                                    .put(Constant.ERROR, eventBusHandler.cause().getMessage()));

                        }
                    } catch (Exception exception) {

                        response(context, 500, new JsonObject()
                                .put(Constant.STATUS, Constant.FAIL)
                                .put(Constant.ERROR, exception.getMessage()));
                    }
                });
            }


        } catch (Exception exception) {

            response(context, 500, new JsonObject()
                    .put(Constant.STATUS, Constant.FAIL)
                    .put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, exception.getCause().getMessage()));

        }
    }

    private void getById(RoutingContext context) {

        LOG.info("credential get by id fn() called");

        try {

            var query = "Select * from credential where credential_id =" + context.pathParam(Constant.ID) + ";";

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject()
                    .put(Constant.QUERY, query)
                    .put(Constant.METHOD_TYPE, Constant.GET_DATA)
                    .put(Constant.REQUEST_POINT, Constant.CREDENTIAL), eventBusHandler -> {

                try {

                    if (eventBusHandler.succeeded()) {

                        var result = eventBusHandler.result().body().getValue(Constant.DATA);

                        response(context, 200, new JsonObject()
                                .put(Constant.STATUS, Constant.SUCCESS).put(Constant.RESULT, result));

                    } else {

                        response(context, 200, new JsonObject()
                                .put(Constant.STATUS, Constant.FAIL)
                                .put(Constant.ERROR, eventBusHandler.cause().getMessage()));

                    }
                } catch (Exception exception) {

                    response(context, 500, new JsonObject()
                            .put(Constant.STATUS, Constant.FAIL)
                            .put(Constant.ERROR, exception.getMessage()));

                }
            });

        } catch (Exception exception) {

            response(context, 400, new JsonObject()
                    .put(Constant.STATUS, Constant.FAIL)
                    .put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, exception.getMessage()));

        }
    }

    private void get(RoutingContext context) {

        LOG.info("credential get all fn() called");

        try {
            var query = "Select * from credential;";

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS,
                    new JsonObject().put(Constant.QUERY, query)
                            .put(Constant.METHOD_TYPE, Constant.GET_DATA),
                    eventBusHandler -> {

                        try {

                            if (eventBusHandler.succeeded()) {

                                var result = eventBusHandler.result().body()
                                        .getJsonArray(Constant.DATA);

                                response(context, 200, new JsonObject()
                                        .put(Constant.STATUS, Constant.SUCCESS)
                                        .put(Constant.RESULT, result));

                            } else {

                                response(context, 400, new JsonObject()
                                        .put(Constant.STATUS, Constant.FAIL)
                                        .put(Constant.ERROR, eventBusHandler.cause().getMessage()));

                            }
                        } catch (Exception exception) {

                            response(context, 500, new JsonObject()
                                    .put(Constant.STATUS, Constant.FAIL)
                                    .put(Constant.ERROR, exception.getMessage()));

                        }
                    });

        } catch (Exception exception) {

            response(context, 400, new JsonObject()
                    .put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, exception.getMessage()));
        }
    }

    private void delete(RoutingContext context) {

        LOG.info("credential delete function called");
        try {

            var query = "delete from credential where credential_id ="
                    + context.pathParam(Constant.ID) + ";";

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject()
                            .put(Constant.QUERY, query)
                            .put(Constant.METHOD_TYPE, Constant.DELETE_DATA),
                    eventBusHandler -> {
                        try {


                            if (eventBusHandler.succeeded()) {
                                var deleteResult = eventBusHandler.result().body();

                                if (deleteResult != null) {
                                    response(context, 200, new JsonObject()
                                            .put(Constant.MESSAGE, "id "
                                                    + context.pathParam(Constant.ID) + " deleted")
                                            .put(Constant.STATUS, Constant.SUCCESS));
                                }

                            } else {

                                response(context, 400, new JsonObject()
                                        .put(Constant.STATUS, Constant.FAIL)
                                        .put(Constant.ERROR, eventBusHandler.result().body()));

                            }
                        } catch (Exception exception) {

                            response(context, 500, new JsonObject()
                                    .put(Constant.STATUS, Constant.FAIL)
                                    .put(Constant.ERROR, exception.getMessage()));

                        }
                    });

        } catch (Exception exception) {

            response(context, 400, new JsonObject()
                    .put(Constant.STATUS, Constant.FAIL)
                    .put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, exception.getMessage()));
        }
    }

    private void update(RoutingContext context) {

        LOG.info("credential update fn () called ");

        try {

            var userData = context.getBodyAsJson();

            var id = userData.getInteger(Constant.CREDENTIAL_ID);

            userData.remove(Constant.CREDENTIAL_ID);

            var queryStatement = new StringBuilder("");

            userData.forEach(value -> {

                var key = value.getKey().replace(".", "_");
                queryStatement.append(key).append(" = ").append("\"")
                        .append(value.getValue()).append("\"").append(",");

            });
            var query = "update credential set "
                    + queryStatement.substring(0, queryStatement.length() - 1)
                    + " where credential_id = " + id + ";";

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject()
                            .put(Constant.QUERY, query)
                            .put(Constant.METHOD_TYPE, Constant.UPDATE_DATA)
                            .put(Constant.REQUEST_POINT, Constant.CREDENTIAL),
                    eventBusHandler -> {
                        try {

                            var result = eventBusHandler.result().body();

                            if (eventBusHandler.succeeded() || result != null) {

                                response(context, 200, new JsonObject()
                                        .put(Constant.MESSAGE, id + " updated")
                                        .put(Constant.STATUS, Constant.SUCCESS));

                            } else {

                                response(context, 400, new JsonObject()
                                        .put(Constant.STATUS, Constant.FAIL)
                                        .put(Constant.ERROR, eventBusHandler.result().body()));

                            }

                        } catch (Exception exception) {

                            response(context, 500, new JsonObject()
                                    .put(Constant.STATUS, Constant.FAIL)
                                    .put(Constant.ERROR, exception.getMessage()));

                        }
                    });
        } catch (Exception exception) {

            response(context, 400, new JsonObject()
                    .put(Constant.STATUS, Constant.FAIL)
                    .put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, exception.getMessage()));

        }

    }

    private void create(RoutingContext context) {

        LOG.info("Credential create called....");

        try {
            StringBuilder values = new StringBuilder();
            var userData = context.getBodyAsJson();

            if (userData.getString(Constant.PROTOCOL).equals(Constant.SSH)
                    || userData.getString(Constant.PROTOCOL).equals(Constant.POWERSHELL)) {

                values
                        .append("insert into credential(credential_name,protocol,username,password)values(")
                        .append("'").append(userData.getString(Constant.CREDENTIAL_NAME).trim())
                        .append("','").append(userData.getString(Constant.PROTOCOL).trim()).append("','")
                        .append(userData.getString(Constant.USERNAME).trim()).append("','")
                        .append(userData.getString(Constant.PASSWORD).trim()).append("');");


            } else if (userData.getString(Constant.PROTOCOL).equals(Constant.SNMP)) {

                values
                        .append("insert into credential(credential_name,protocol,community,version)values(")
                        .append("'").append(userData.getString(Constant.CREDENTIAL_NAME).trim())
                        .append("','").append(userData.getString(Constant.PROTOCOL).trim()).append("','")
                        .append(userData.getString(Constant.COMMUNITY).trim()).append("','")
                        .append(userData.getString(Constant.VERSION).trim()).append("');");

            }

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS,
                    new JsonObject().put(Constant.QUERY, values)
                            .put(Constant.METHOD_TYPE, Constant.CREATE_DATA)
                            .put(Constant.REQUEST_POINT, Constant.CREDENTIAL),

                    eventBusHandler -> {

                        try {

                            if (eventBusHandler.succeeded()) {

                                var handlerResult = eventBusHandler.result().body()
                                        .getJsonArray(Constant.DATA);

                                var id = handlerResult.getJsonObject(0).getInteger(Constant.ID);

                                response(context, 200, new JsonObject()
                                        .put(Constant.MESSAGE, "credential created successfully")
                                        .put(Constant.STATUS, Constant.SUCCESS).put(Constant.ID, id));

                            } else {

                                response(context, 400, new JsonObject()
                                        .put(Constant.STATUS, Constant.FAIL)
                                        .put(Constant.ERROR, eventBusHandler.cause().getMessage()));
                            }
                        } catch (Exception exception) {

                            response(context, 500, new JsonObject()
                                    .put(Constant.STATUS, Constant.FAIL)
                                    .put(Constant.ERROR, exception.getMessage()));

                        }
                    });

        } catch (Exception e) {

            response(context, 400, new JsonObject()
                    .put(Constant.STATUS, Constant.FAIL)
                    .put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, e.getMessage()));

        }
    }

    private void validate(RoutingContext context) {

        LOG.info("credential validate fn() called");

        try {

            if ((context.request().method() != HttpMethod.DELETE)
                    && (context.request().method() != HttpMethod.GET)) {

                if (context.getBodyAsJson() == null) {

                    response(context, 400, new JsonObject()
                            .put(Constant.STATUS, Constant.FAIL)
                            .put(Constant.MESSAGE, "json is required"));

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

                case "DELETE" -> validateDelete(context);

                case "GET" -> validateGet(context);

                default -> response(context, 400, new JsonObject()
                        .put(Constant.STATUS, Constant.FAIL)
                        .put(Constant.MESSAGE, "wrong method"));

            }

        } catch (Exception exception) {

            response(context, 400, new JsonObject()
                    .put(Constant.STATUS, Constant.FAIL)
                    .put(Constant.MESSAGE, "wrong json format")
                    .put(Constant.ERROR, exception.getMessage()));

        }

    }

    private void validateGet(RoutingContext context) {


        if (context.pathParam(Constant.ID) == null) {
            response(context, 400, new JsonObject()
                    .put(Constant.MESSAGE, "id is null").put(Constant.STATUS, Constant.FAIL));

        } else {

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject()
                    .put(Constant.ID, Integer.parseInt(context.pathParam(Constant.ID)))
                    .put(Constant.REQUEST_POINT, Constant.CREDENTIAL)
                    .put(Constant.METHOD_TYPE, Constant.CHECK_ID), eventBusHandler -> {

                try {

                    if (eventBusHandler.succeeded() && eventBusHandler.result().body() != null) {

                        if (eventBusHandler.succeeded()) {

                            if (eventBusHandler.result().body().getString(Constant.STATUS).equals(Constant.FAIL)) {

                                response(context, 400, new JsonObject()
                                        .put(Constant.ERROR, eventBusHandler.result().body().getString(Constant.ERROR))
                                        .put(Constant.STATUS, Constant.FAIL));

                            } else {

                                context.next();

                            }

                        }

                    } else {

                        response(context, 400, new JsonObject()
                                .put(Constant.STATUS, Constant.FAIL)
                                .put(Constant.ERROR, eventBusHandler.cause().getMessage()));

                    }
                } catch (Exception exception) {

                    response(context, 500, new JsonObject()
                            .put(Constant.STATUS, Constant.FAIL)
                            .put(Constant.ERROR, exception.getMessage()));

                }
            });
        }
    }

    private void validateDelete(RoutingContext context) {

        if (context.pathParam(Constant.ID) == null) {

            response(context, 400, new JsonObject()
                    .put(Constant.MESSAGE, "id is null").put(Constant.STATUS, Constant.FAIL));

        } else {

            eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS, new JsonObject()
                    .put(Constant.ID, Integer.parseInt(context.pathParam(Constant.ID)))
                    .put(Constant.REQUEST_POINT, Constant.CREDENTIAL)
                    .put(Constant.METHOD_TYPE, Constant.CHECK_CREDENTIAL_DELETE), eventBusHandler -> {
                try {

                    if (eventBusHandler.succeeded()) {
                        if (eventBusHandler.result().body().getString(Constant.STATUS).equals(Constant.FAIL)) {

                            response(context, 400, new JsonObject()
                                    .put(Constant.ERROR, eventBusHandler.result().body().getString(Constant.ERROR))
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
                            .put(Constant.ERROR, exception.getMessage()));
                }
            });
        }
    }

    private void validateUpdate(RoutingContext context) {
        var errors = new ArrayList<String>();

        eventBus.<JsonObject>request(Constant.DATABASE_EVENTBUS_ADDRESS,
                context.getBodyAsJson().put(Constant.METHOD_TYPE, Constant.CHECK_CREDENTIAL_UPDATES)
                        .put(Constant.REQUEST_POINT, Constant.CREDENTIAL), eventBusHandler -> {
                    try {

                        if (eventBusHandler.succeeded() && eventBusHandler.result().body() != null) {

                            if (eventBusHandler.result().body().getString(Constant.STATUS).equals(Constant.FAIL)) {

                                response(context, 400, new JsonObject().put(Constant.MESSAGE,
                                                eventBusHandler.result().body().getString(Constant.ERROR))
                                        .put(Constant.STATUS, Constant.FAIL));

                            } else {

                                var validationData = eventBusHandler.result().body().getJsonArray(Constant.DATA);

                                if (validationData.getJsonObject(0).getString(Constant.PROTOCOL).equals(Constant.SSH)
                                        || validationData.getJsonObject(0).getString(Constant.PROTOCOL)
                                        .equals(Constant.POWERSHELL)) {

                                    if (context.getBodyAsJson().containsKey(Constant.USERNAME)
                                            && (context.getBodyAsJson().getString(Constant.USERNAME).isEmpty()
                                            || !(context.getBodyAsJson().getValue(Constant.USERNAME) instanceof String))) {

                                        errors.add("username is required");
                                    }
                                    if (context.getBodyAsJson().containsKey(Constant.PASSWORD)
                                            && (context.getBodyAsJson().getString(Constant.PASSWORD).isEmpty()
                                            || !(context.getBodyAsJson().getValue(Constant.PASSWORD) instanceof String))) {

                                        errors.add("password is required");
                                    }
                                }
                                if (validationData.getJsonObject(0).getString(Constant.PROTOCOL)
                                        .equals(Constant.SNMP)) {

                                    if (context.getBodyAsJson().containsKey(Constant.COMMUNITY)
                                            && (context.getBodyAsJson().getString(Constant.COMMUNITY).isEmpty()
                                            || !(context.getBodyAsJson().getValue(Constant.COMMUNITY) instanceof String))) {

                                        errors.add("community is required");
                                    }

                                    if (context.getBodyAsJson().containsKey(Constant.VERSION)
                                            && (context.getBodyAsJson().getString(Constant.VERSION).isEmpty()
                                            || !(context.getBodyAsJson().getValue(Constant.VERSION) instanceof String))) {

                                        errors.add("version is required");
                                    }
                                }

                                if (errors.isEmpty()) {

                                    context.next();

                                } else {

                                    response(context, 400, new JsonObject()
                                            .put(Constant.MESSAGE, errors).put(Constant.STATUS, Constant.FAIL));

                                }
                            }
                        } else {

                            response(context, 500, new JsonObject()
                                    .put(Constant.STATUS, Constant.FAIL)
                                    .put(Constant.ERROR, eventBusHandler.cause().getMessage()));

                        }
                    } catch (Exception exception) {

                        response(context, 500, new JsonObject()
                                .put(Constant.STATUS, Constant.FAIL)
                                .put(Constant.ERROR, exception.getMessage()));

                    }
                });
    }

    private void validateCreate(RoutingContext context) {
        var errors = new ArrayList<String>();

        if (!context.getBodyAsJson().containsKey(Constant.CREDENTIAL_NAME)
                || !(context.getBodyAsJson().getValue(Constant.CREDENTIAL_NAME) instanceof String)
                || context.getBodyAsJson().getString(Constant.CREDENTIAL_NAME).isEmpty()) {

            errors.add("credential.name is required");
            LOG.error("credential.name is required");
        }

        if (!context.getBodyAsJson().containsKey(Constant.PROTOCOL)
                || context.getBodyAsJson().getString(Constant.PROTOCOL).isEmpty()
                || !(context.getBodyAsJson().getValue(Constant.PROTOCOL) instanceof String)) {

            errors.add("protocol is required");
            LOG.error("protocol is required");

        } else if (!context.getBodyAsJson().getString(Constant.PROTOCOL).equals(Constant.SSH)
                && !context.getBodyAsJson().getString(Constant.PROTOCOL).equals(Constant.POWERSHELL)
                && !context.getBodyAsJson().getString(Constant.PROTOCOL).equals(Constant.SNMP)) {

            errors.add("wrong protocol");

        } else if (context.getBodyAsJson().getString(Constant.PROTOCOL).equals(Constant.SSH)
                || context.getBodyAsJson().getString(Constant.PROTOCOL).equals(Constant.POWERSHELL)) {

            if (!context.getBodyAsJson().containsKey(Constant.USERNAME)
                    || context.getBodyAsJson().getString(Constant.USERNAME).isEmpty()
                    || !(context.getBodyAsJson().getValue(Constant.USERNAME) instanceof String)) {

                errors.add("username is required");

            }

            if (!context.getBodyAsJson().containsKey(Constant.PASSWORD)
                    || context.getBodyAsJson().getString(Constant.PASSWORD).isEmpty()
                    || !(context.getBodyAsJson().getValue(Constant.PASSWORD) instanceof String)) {

                errors.add("password is required");

            }

        } else if (context.getBodyAsJson().getString(Constant.PROTOCOL).equals(Constant.SNMP)) {


            if (!context.getBodyAsJson().containsKey(Constant.COMMUNITY)
                    || context.getBodyAsJson().getString(Constant.COMMUNITY).isEmpty()
                    || !(context.getBodyAsJson().getValue(Constant.COMMUNITY) instanceof String)) {

                errors.add("community is required");

            }

            if (!context.getBodyAsJson().containsKey(Constant.VERSION)
                    || context.getBodyAsJson().getString(Constant.VERSION).isEmpty()
                    || !(context.getBodyAsJson().getValue(Constant.VERSION) instanceof String)) {

                errors.add("version is required");

            }
        }

        if (errors.isEmpty()) {

            eventBus.request(Constant.DATABASE_EVENTBUS_ADDRESS, context.getBodyAsJson()
                    .put(Constant.METHOD_TYPE, Constant.CHECK_MULTI_FIELDS)
                    .put(Constant.REQUEST_POINT, Constant.CREDENTIAL), eventBusHandler -> {
                try {

                    if (eventBusHandler.succeeded()) {


                        context.next();

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
