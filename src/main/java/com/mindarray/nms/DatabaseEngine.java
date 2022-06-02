package com.mindarray.nms;

import com.mindarray.Constant;
import com.mysql.cj.jdbc.result.ResultSetMetaData;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class DatabaseEngine extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseEngine.class.getName());

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        var eventBus = vertx.eventBus();

        init();

        eventBus.<JsonObject>localConsumer(Constant.DATABASE_EVENTBUS_ADDRESS, databaseEventBusHandler -> {

            try {

                var databaseData = databaseEventBusHandler.body();
                var futures = new ArrayList<Future>();

                switch (databaseData.getString(Constant.METHOD_TYPE)) {

                    case Constant.CHECK_MULTI_FIELDS -> {

                        if (databaseData.getString(Constant.REQUEST_POINT).equals(Constant.DISCOVERY)) {

                            if (databaseData.containsKey(Constant.DISCOVERY_NAME)) {

                                var nameCheck = check(Constant.DISCOVERY,
                                        "discovery_name",
                                        databaseData.getString(Constant.DISCOVERY_NAME).trim());
                                futures.add(nameCheck);

                            }

                            if (databaseData.containsKey(Constant.CREDENTIAL_PROFILE)) {

                                var credentialID = check(Constant.CREDENTIAL,
                                        "credential_id",
                                        databaseData.getValue(Constant.CREDENTIAL_PROFILE));
                                futures.add(credentialID);

                            }

                        } else if (databaseData.getString(Constant.REQUEST_POINT).equals(Constant.CREDENTIAL)) {

                            var nameCheck = check(Constant.CREDENTIAL,
                                    "credential_name",
                                    databaseData.getString(Constant.CREDENTIAL_NAME).trim());
                            futures.add(nameCheck);

                        }

                        CompositeFuture.join(futures).onComplete(futureCompleteHandler -> {
                            try {

                                if (futureCompleteHandler.succeeded()) {

                                    databaseEventBusHandler.reply(databaseEventBusHandler.body()
                                            .put(Constant.STATUS, Constant.SUCCESS));

                                } else {

                                    databaseEventBusHandler.fail(-1,
                                            futureCompleteHandler.cause().getMessage());

                                }
                            }catch (Exception exception){

                                databaseEventBusHandler.fail(-1, exception.getCause().getMessage());

                            }
                        });
                    }


                    case Constant.CREATE_DATA -> {

                        var execute = executeQuery(databaseData.getString(Constant.QUERY));

                        execute.onComplete(futureCompleteHandler -> {
                            try {


                                if (futureCompleteHandler.failed()) {

                                    databaseEventBusHandler.fail(-1,
                                            futureCompleteHandler.cause().getMessage());

                                } else {

                                    if (databaseData.getString(Constant.REQUEST_POINT).equals(Constant.DISCOVERY)) {

                                        String query = "select max(discovery_id) as id  from discovery;";
                                        var getId = getQuery(query);

                                        getId.onComplete(queryCompleteHandler -> {
                                            try{

                                            if (queryCompleteHandler.failed()) {

                                                databaseEventBusHandler.fail(-1,
                                                        queryCompleteHandler.cause().getMessage());

                                            } else {

                                                databaseEventBusHandler.reply(queryCompleteHandler.result()
                                                        .put(Constant.STATUS, Constant.SUCCESS));

                                            }
                                        }catch (Exception exception){
                                            databaseEventBusHandler.fail(-1,
                                                    exception.getCause().getMessage());
                                        }
                                        });

                                    } else if (databaseData.getString(Constant.REQUEST_POINT)
                                            .equals(Constant.CREDENTIAL)) {

                                        String query = "select max(credential_id) as id  from credential;";
                                        var getId = getQuery(query);

                                        getId.onComplete(queryCompleteHandler -> {
                                            try{

                                            if (queryCompleteHandler.failed()) {
                                                databaseEventBusHandler.fail(-1,
                                                        queryCompleteHandler.cause().getMessage());

                                            } else {

                                                databaseEventBusHandler.reply(queryCompleteHandler.result()
                                                        .put(Constant.STATUS, Constant.SUCCESS));

                                            }
                                        }catch (Exception exception){
                                                databaseEventBusHandler.fail(-1,
                                                        exception.getCause().getMessage());
                                            }
                                        });

                                    } else if (databaseData.getString(Constant.REQUEST_POINT)
                                            .equals(Constant.MONITOR)) {

                                        String query = "select max(monitor_id) as id  from monitor;";
                                        var getId = getQuery(query);

                                        getId.onComplete(queryCompleteHandler -> {
                                            try {


                                                if (queryCompleteHandler.failed()) {
                                                    databaseEventBusHandler.fail(-1,
                                                            queryCompleteHandler.cause().getMessage());

                                                } else {

                                                    databaseEventBusHandler.reply(queryCompleteHandler.result()
                                                            .put(Constant.STATUS, Constant.SUCCESS));

                                                }
                                            }catch (Exception exception){
                                                databaseEventBusHandler.fail(-1,
                                                        exception.getCause().getMessage());
                                            }

                                        });
                                    }

                                }
                            }catch (Exception exception){
                                databaseEventBusHandler.fail(-1, exception.getCause().getMessage());
                            }
                        });
                    }


                    case Constant.CHECK_DISCOVERY_UPDATES -> {

                        if (databaseData.containsKey(Constant.DISCOVERY_NAME)) {

                            var nameCheck = check(Constant.DISCOVERY, "discovery_name",
                                    databaseData.getString(Constant.DISCOVERY_NAME).trim());
                            futures.add(nameCheck);

                        }
                        if (databaseData.containsKey(Constant.DISCOVERY_ID)) {

                            var idCheck = check(Constant.DISCOVERY, "discovery_id",
                                    databaseData.getString(Constant.DISCOVERY_ID));
                            futures.add(idCheck);

                        }
                        if (databaseData.containsKey(Constant.CREDENTIAL_PROFILE)) {

                            var credentialCheck = check(Constant.CREDENTIAL, "credential_id",
                                    databaseData.getString(Constant.CREDENTIAL_PROFILE));
                            futures.add(credentialCheck);

                        }

                        CompositeFuture.join(futures).onComplete(futureCompleteHandler -> {
                            try {

                                if (futureCompleteHandler.succeeded()) {

                                    databaseEventBusHandler.reply(databaseEventBusHandler.body()
                                            .put(Constant.STATUS, Constant.SUCCESS));

                                } else {

                                    databaseEventBusHandler.fail(-1,
                                            futureCompleteHandler.cause().getMessage());

                                }
                            }catch (Exception exception){
                                databaseEventBusHandler.fail(-1, exception.getCause().getMessage());
                            }
                        });
                    }


                    case Constant.CHECK_CREDENTIAL_UPDATES -> {

                        if (databaseData.containsKey(Constant.CREDENTIAL_ID)) {

                            if (databaseData.containsKey(Constant.CREDENTIAL_ID)) {

                                var idChecker = check(Constant.CREDENTIAL, "credential_id",
                                        databaseData.getInteger(Constant.CREDENTIAL_ID));

                                futures.add(idChecker);

                            }

                            if (databaseData.containsKey(Constant.CREDENTIAL_NAME)) {

                                var nameCheck = check(Constant.CREDENTIAL,
                                        "credential_name",
                                        databaseData.getString(Constant.CREDENTIAL_NAME).trim());

                                futures.add(nameCheck);

                            }
                        }

                        CompositeFuture.join(futures).<JsonObject>onComplete(futureCompleteHandler -> {
                            try {

                                if (futureCompleteHandler.succeeded()) {

                                    String query = "select * from credential where credential_id="
                                            + databaseData.getInteger(Constant.CREDENTIAL_ID) + ";";
                                    var data = getQuery(query);

                                    data.onComplete(queryCompleteHandler -> {
                                        try {

                                            if (queryCompleteHandler.failed()) {
                                                databaseEventBusHandler.fail(-1,
                                                        queryCompleteHandler.cause().getMessage());

                                            } else {

                                                databaseEventBusHandler.reply(queryCompleteHandler.result()
                                                        .put(Constant.STATUS, Constant.SUCCESS));

                                            }
                                        } catch (Exception exception) {

                                            databaseEventBusHandler.fail(-1,
                                                    exception.getCause().getMessage());
                                        }
                                    });

                                } else {

                                    databaseEventBusHandler.fail(-1,
                                            futureCompleteHandler.cause().getMessage());

                                }
                            } catch (Exception exception){

                                databaseEventBusHandler.fail(-1,
                                        exception.getCause().getMessage());
                            }
                        });
                    }


                    case Constant.UPDATE_DATA, Constant.DELETE_DATA, Constant.METRIC_CREATE -> {

                        var execute = executeQuery(databaseData.getString(Constant.QUERY));

                        execute.onComplete(futureCompleteHandler -> {
                            try {

                                if (futureCompleteHandler.failed()) {

                                    databaseEventBusHandler.fail(-1,
                                            futureCompleteHandler.cause().getMessage());

                                } else {

                                    databaseEventBusHandler.reply(new JsonObject()
                                            .put(Constant.STATUS, Constant.SUCCESS));

                                }
                            }catch (Exception exception){
                                databaseEventBusHandler.fail(-1 , exception.getCause().getMessage());
                            }
                        });
                    }


                    case Constant.CHECK_CREDENTIAL_DELETE -> {

                        StringBuilder error = new StringBuilder();

                        if (databaseData.containsKey(Constant.ID)) {

                            var checkInCredential = check(Constant.CREDENTIAL,
                                    "credential_id", databaseData.getInteger(Constant.ID));
                            futures.add(checkInCredential);

                            var checkInDiscovery = check(Constant.DISCOVERY,
                                    "credential_profile", databaseData.getInteger(Constant.ID));
                            futures.add(checkInDiscovery);

                            var checkInMetric = check(Constant.METRIC,
                                    "credential_profile", databaseData.getInteger(Constant.ID));
                            futures.add(checkInMetric);
                        }
                        CompositeFuture.join(futures).onComplete(futureCompleteHandler -> {
                            try {
                                if (futureCompleteHandler.failed()) {
                                    if (futures.get(0).failed()) {
                                        databaseEventBusHandler.fail(-1,
                                                futureCompleteHandler.cause().getMessage());

                                    } else {

                                        if (futures.get(1).succeeded()) {
                                            error.append(" credential.id present in discovery table ");
                                        }

                                        if (futures.get(2).succeeded()) {
                                            error.append(" credential.id used in monitor for polling");
                                        }

                                        if (error.isEmpty()) {
                                            databaseEventBusHandler.reply(databaseEventBusHandler.body()
                                                    .put(Constant.STATUS, Constant.SUCCESS));

                                        } else {
                                            databaseEventBusHandler.fail(-1, error.toString());
                                        }
                                    }

                                } else {

                                    databaseEventBusHandler.fail(-1,
                                            "credential.id exists in discovery and monitor table");

                                }
                            }catch (Exception exception){

                                databaseEventBusHandler.fail(-1,
                                        exception.getCause().getMessage());

                            }
                        });
                    }


                    case Constant.CHECK_ID -> {

                        if (databaseData.getString(Constant.REQUEST_POINT).equals(Constant.DISCOVERY)) {

                            var idCheck = check(Constant.DISCOVERY,
                                    "discovery_id", databaseData.getString(Constant.ID));
                            futures.add(idCheck);

                        }
                        if (databaseData.getString(Constant.REQUEST_POINT).equals(Constant.MONITOR)) {

                            var idCheck = check(Constant.MONITOR,
                                    "monitor_id", databaseData.getString(Constant.ID));
                            futures.add(idCheck);

                        }
                        if (databaseData.getString(Constant.REQUEST_POINT).equals(Constant.CREDENTIAL)) {

                            var checkInCredential = check(Constant.CREDENTIAL,
                                    "credential_id", databaseData.getInteger(Constant.ID));
                            futures.add(checkInCredential);

                        }

                        if (databaseData.getString(Constant.REQUEST_POINT).equals(Constant.METRIC)) {

                            var idCheck = check(Constant.METRIC,
                                    Constant.ID, databaseData.getString(Constant.ID));
                            futures.add(idCheck);

                        }
                        CompositeFuture.join(futures).onComplete(futureCompleteHandler -> {
                            try {

                                if (futureCompleteHandler.failed()) {
                                    databaseEventBusHandler.fail(-1,
                                            futureCompleteHandler.cause().getMessage());

                                } else {

                                    databaseEventBusHandler.reply(databaseEventBusHandler.body()
                                            .put(Constant.STATUS, Constant.SUCCESS));

                                }
                            }catch (Exception exception){
                                databaseEventBusHandler.fail(-1, exception.getCause().getMessage());
                            }
                        });
                    }


                    case Constant.GET_DATA, Constant.RUN_CHECK_DATA -> {

                        var execute = getQuery(databaseData.getString(Constant.QUERY));

                        execute.onComplete(futureCompleteHandler -> {
                            try {

                                if (futureCompleteHandler.failed()) {

                                    databaseEventBusHandler.fail(-1,
                                            futureCompleteHandler.cause().getMessage());

                                } else {

                                    databaseEventBusHandler.reply(futureCompleteHandler.result());

                                }
                            }catch (Exception exception){
                                databaseEventBusHandler.fail(-1 , exception.getCause().getMessage());
                            }
                        });
                    }


                    case Constant.DISCOVERY_RESULT_INSERT -> {

                        var insertDiscovery = executeQuery(databaseData.getString(Constant.QUERY));

                        insertDiscovery.onComplete(futureCompleteHandler -> {
                            try {

                                if (futureCompleteHandler.succeeded()) {

                                    databaseEventBusHandler.reply(new JsonObject()
                                            .put(Constant.STATUS, Constant.SUCCESS));

                                } else {

                                    databaseEventBusHandler.fail(-1,
                                            futureCompleteHandler.cause().getMessage());

                                }
                            }catch (Exception exception){

                                databaseEventBusHandler.fail(-1,
                                        exception.getCause().getMessage());
                            }
                        });
                    }


                    case Constant.MONITOR_CHECK -> {

                        String ipTYpeQuery = "select exists(select * from monitor where ip=" + "'"
                                + databaseData.getString(Constant.IP)
                                + "'" + " and type='" + databaseData.getString(Constant.TYPE) + "'" + ") as ip;";

                        getQuery(ipTYpeQuery).onComplete(futureCompleteHandler -> {

                            if (futureCompleteHandler.succeeded()) {

                                var checkIPData = futureCompleteHandler.result()
                                        .getJsonArray(Constant.DATA);

                                if (checkIPData.getJsonObject(0).containsKey(Constant.IP)
                                        && checkIPData.getJsonObject(0).getInteger(Constant.IP)
                                        .equals(0)) {

                                    String checkDiscoveryStatus = "select exists(select discovery_id from discovery where ip="
                                            + "'" + databaseData.getString(Constant.IP) + "'"
                                            + " and Json_search(result,'one','success') and credential_profile="
                                            + databaseData.getInteger(Constant.CREDENTIAL_ID)
                                            + " and type="
                                            + "'" + databaseData.getString(Constant.TYPE) + "'"
                                            + ") as discoveryStatus;";

                                    getQuery(checkDiscoveryStatus).onComplete(checkDiscoveryStatusFutureHandler -> {

                                        if (checkDiscoveryStatusFutureHandler.succeeded()) {

                                            var checkData = checkDiscoveryStatusFutureHandler.result()
                                                    .getJsonArray(Constant.DATA);

                                            if (checkData.getJsonObject(0).containsKey("discoveryStatus")
                                                    && !checkData.getJsonObject(0)
                                                    .getInteger("discoveryStatus").equals(0)) {

                                                databaseEventBusHandler.reply(
                                                        databaseEventBusHandler.body()
                                                                .put(Constant.STATUS, Constant.SUCCESS)
                                                                .put(Constant.MESSAGE,
                                                                        checkDiscoveryStatusFutureHandler.result()));

                                            } else {

                                                databaseEventBusHandler.fail(-1,
                                                        "data mismatched or discovery not successful");

                                            }

                                        } else {

                                            databaseEventBusHandler.fail(-1,
                                                    checkDiscoveryStatusFutureHandler.cause().getMessage());

                                        }

                                    });

                                } else {

                                    databaseEventBusHandler.fail(-1,
                                            "monitor already exists");

                                }

                            } else {

                                databaseEventBusHandler.fail(-1,
                                        futureCompleteHandler.cause().getMessage());

                            }
                        });
                    }


                    case Constant.MONITOR_DELETE -> {

                        var execute = executeQuery(databaseData.getString(Constant.QUERY));

                        execute.onComplete(futureCompleteHandler -> {

                            if (futureCompleteHandler.failed()) {

                                databaseEventBusHandler.fail(-1,
                                        futureCompleteHandler.cause().getMessage());

                            } else {

                                var id = databaseData.getValue(Constant.MONITOR_ID);
                                String deleteMetricData = "delete from metric where monitor_id =" + id + ";";
                                var deleteQuery = executeQuery(deleteMetricData);

                                deleteQuery.onComplete(queryCompletedHandler -> {
                                    try {

                                        if (queryCompletedHandler.failed()) {
                                            databaseEventBusHandler.fail(-1,
                                                    queryCompletedHandler.cause().getMessage());

                                        } else {

                                            databaseEventBusHandler.reply(new JsonObject()
                                                    .put(Constant.STATUS, Constant.SUCCESS));
                                            LOG.info("metric data also deleted");

                                        }
                                    }catch (Exception exception){
                                        databaseEventBusHandler.fail( -1,
                                                exception.getCause().getMessage());
                                    }
                                });
                            }
                        });
                    }


                    case Constant.INSERT_POLLED_DATA -> {

                        var data = databaseData.getJsonObject("pollingResult");

                        if (data.isEmpty()) {

                            LOG.error("polling result is empty");

                        } else {
                            LOG.info("data is ->{}", data);

                            String insertPollingQuery = "insert into polling(monitor_id,timestamp,data,metric_group)values('"
                                    + data.getInteger(Constant.MONITOR_ID) + "','"
                                    + data.getLong("timestamp") + "','"
                                    + (data.getValue(Constant.RESULT)) + "','"
                                    + data.getString(Constant.METRIC_GROUP) + "');";

                            executeQuery(insertPollingQuery).onComplete(futureCompleteHandler -> {
                                try {

                                    if (futureCompleteHandler.succeeded()) {

                                        LOG.info("data is inserted in polling table");

                                    } else {

                                        LOG.error(futureCompleteHandler.cause().getMessage());
                                    }
                                }
                                catch (Exception exception){

                                    LOG.warn("EXCEPTION->{}",exception.getCause().getMessage());

                                }
                            });
                        }
                    }


                    default -> databaseEventBusHandler.fail(-1, "wrong request");
                }
            }
            catch (Exception exception){

                databaseEventBusHandler.fail(-1, exception.getCause().getMessage());

            }
        });

        startPromise.complete();
    }


    private void init() {

        LOG.info("DatabaseEngine init called...");

        vertx.executeBlocking(blockingHandler -> {

            try (var conn = connection(); var smt = conn.createStatement()) {

                smt.execute("CREATE DATABASE IF NOT EXISTS nms;");

                smt.execute("use nms");

                smt.execute("create table if not exists metric_values(type varchar(90) not null,metric_group varchar(90) not null, time int);");

                smt.execute("create table if not exists discovery(discovery_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, discovery_name varchar(255) NOT NULL UNIQUE, ip varchar(90) NOT NULL, type varchar(90) NOT NULL, credential_profile int NOT NULL, port int NOT NULL, result json);");

                smt.execute("create table if not exists monitor(monitor_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, ip varchar(90) NOT NULL, host varchar(255) NOT NULL ,type varchar(90) NOT NULL,  port int NOT NULL);");

                smt.execute("CREATE TABLE IF NOT EXISTS credential (credential_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, credential_name varchar(255) NOT NULL UNIQUE,protocol varchar(90) NOT NULL, username varchar(255), password varchar(255), community varchar(90), version varchar(50));");

                smt.execute("create table if not exists metric(id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,monitor_id int,credential_profile int,metric_group varchar(90),time int,objects json);");

                smt.execute("create table if not exists polling(id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,monitor_id int,timestamp long,data json,metric_group varchar(90));");

            } catch (SQLException sqlException) {

                LOG.error(sqlException.getCause().getMessage());

            }

            blockingHandler.complete();

        }).onComplete(completeHandler -> {

            var metricQuery = "select * from metric_values;";

            var metricTableCheck = getQuery(metricQuery);

            metricTableCheck.onComplete(futureCompleteHandler -> {

                if (futureCompleteHandler.succeeded()
                        && !futureCompleteHandler.result().getJsonArray(Constant.DATA).isEmpty()) {

                    LOG.debug("metric_values not empty");

                } else {

                    try (var conn = connection(); var smt = conn.createStatement()) {

                        smt.execute("use nms");

                        smt.execute("insert into metric_values(type,metric_group,time)values(\"linux\",\"ping\",60000),(\"linux\",\"cpu\",70000),(\"linux\",\"process\",90000),(\"linux\",\"memory\",100000),(\"linux\",\"disk\",120000),(\"linux\",\"system\",150000),(\"windows\",\"ping\",60000),(\"windows\",\"cpu\",70000),(\"windows\",\"process\",95000),(\"windows\",\"memory\",120000),(\"windows\",\"disk\",135000),(\"windows\",\"system\",160000),(\"network\",\"ping\",60000),(\"network\",\"interface\",90000),(\"network\",\"system\",140000);");

                    } catch (SQLException sqlException) {

                        LOG.error(sqlException.getCause().getMessage());

                    }
                }
            });
        });
    }

    private Future<JsonObject> check(String table, String column, Object value) {

        var errors = new ArrayList<>();

        Promise<JsonObject> promise = Promise.promise();

        if (table == null || column == null || value == null) {

            errors.add("data is null");

        } else {

            vertx.<JsonObject>executeBlocking(queryHandler -> {

                try (var conn = connection(); var smt = conn.createStatement()) {

                    smt.execute("use nms");

                    String query = "select exists(select * from " + table + " where "
                            + column + "=" + "\"" + value + "\"" + ");";

                    ResultSet result = smt.executeQuery(query);
                    while (result.next()) {

                        if (column.equals("discovery_name") || column.equals("credential_name")) {

                            if (result.getInt(1) == 1) {

                                errors.add(table + "." + column + " is not unique");
                            }

                        } else {

                            if (result.getInt(1) == 0) {

                                errors.add(table + "." + column + " does not exists in table ");

                            }
                        }
                    }

                } catch (SQLException sqlException) {

                    errors.add(sqlException.getCause().getMessage());

                }

                queryHandler.complete();

            }).onComplete(completeHandler -> {

                if (errors.isEmpty()) {

                    promise.complete(new JsonObject().put(Constant.STATUS, Constant.SUCCESS));

                } else {

                    promise.fail(String.valueOf(errors));

                }

            });
        }

        return promise.future();

    }

    private Future<JsonObject> executeQuery(String query) {

        var errors = new ArrayList<>();

        Promise<JsonObject> promise = Promise.promise();

        vertx.<JsonObject>executeBlocking(queryHandler -> {

            var result = new JsonObject();

            try (var conn = connection(); var smt = conn.createStatement()) {

                smt.execute("use nms");


                var res = smt.execute(query);

                result.put(Constant.RESULT, res);

            } catch (SQLException sqlException) {

                errors.add(sqlException);
            }

            queryHandler.complete(result);

        }).onComplete(completeHandler -> {

            if (errors.isEmpty()) {

                promise.complete(completeHandler.result());

            } else {

                promise.fail(String.valueOf(errors));

            }
        });

        return promise.future();
    }


    private Future<JsonObject> getQuery(String query) {

        var errors = new ArrayList<>();
        Promise<JsonObject> promise = Promise.promise();

        vertx.<JsonObject>executeBlocking(queryHandler -> {

            var resultData = new JsonObject();
            var data = new JsonArray();

            try (var conn = connection(); var smt = conn.createStatement()) {

                smt.execute("use nms");

                ResultSet res = smt.executeQuery(query);
                ResultSetMetaData rsmd = (ResultSetMetaData) res.getMetaData();

                while (res.next()) {

                    var result = new JsonObject();
                    int columns = rsmd.getColumnCount();
                    int index = 1;

                    while (index <= columns) {

                        if (res.getObject(index) != null) {
                            String key = rsmd.getColumnName(index).replace("_", ".");
                            result.put(key, res.getObject(index));
                        }

                        index++;

                    }

                    data.add(result);

                }

            } catch (SQLException sqlException) {

                errors.add(sqlException);
            }

            resultData.put(Constant.DATA, data);

            queryHandler.complete(resultData);

        }).onComplete(completeHandler -> {

            if (errors.isEmpty()) {

                promise.complete(completeHandler.result());

            } else {

                promise.fail(String.valueOf(errors));

            }
        });

        return promise.future();
    }

    private Connection connection() throws SQLException {

        var connection = DriverManager.getConnection("jdbc:mysql://localhost:3306",
                "siddhant", "Sid@mtdt#25");

        LOG.info("Database Connection Successful");

        return connection;

    }

}
