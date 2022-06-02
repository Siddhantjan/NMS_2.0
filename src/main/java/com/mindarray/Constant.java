package com.mindarray;

public class Constant {

    //API endpoints
    public static final String DISCOVERY_POINT = "/discovery";
    public static final String CREDENTIAL_POINT = "/credential";
    public static final String MONITOR_POINT = "/monitor";
    public static final String METRIC_POINT = "/metric";

    //sub endpoints
    public static final String PROVISION_POINT = "/provision";



    //response constants
    public static final String HEADER_TYPE = "content-type";
    public static final String HEADER_VALUE = "application/json";


    //Type 
    public static final String POWERSHELL = "powershell";

    public static final String SSH = "ssh";

    public static final String SNMP = "snmp";

    public static final String NETWORK = "network";

    public static final String WINDOWS = "windows";

    public static final String LINUX = "linux";
    

    //Json Objects constants
    public static final String QUERY = "query";

    public static final String RESULT = "result";
    public static final String MESSAGE = "message";
    public static final String ERROR = "error";
    public static final String STATUS = "status";
    public static final String FAIL = "fail";
    public static final String SUCCESS = "success";
    public static final String DISCOVERY_NAME = "discovery.name";
    public static final String CREDENTIAL_NAME = "credential.name";
    public static final String CREDENTIAL_PROFILE = "credential.profile";
    public static final String COMMUNITY = "community";
    public static final String VERSION = "version";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String IP = "ip";
    public static final String PROTOCOL = "protocol";
    public static final String PORT = "port";
    public static final String TYPE = "type";
    public static final String CREDENTIAL_ID = "credential.id";
    public static final String DISCOVERY_ID = "discovery.id";

    public static final String MONITOR_ID = "monitor.id";

    public static final String ID = "id";

    public static final String HOST = "host";

    public static final String METRIC_GROUP = "metric.group";
    public static final String METHOD_TYPE = "method";
    public static final String REQUEST_POINT = "requester";


    //requester
    public static final String DISCOVERY = "discovery";
    public static final String CREDENTIAL = "credential";
    public static final String MONITOR = "monitor";
    public static final String METRIC = "metric";



    // Functions Name for Engines
    public static final String CHECK_MULTI_FIELDS = "checkFields";
    public static final String CREATE_DATA = "create";
    public static final String CHECK_DISCOVERY_UPDATES = "discoveryUpdates";
    public static final String CHECK_CREDENTIAL_UPDATES = "credentialUpdates";
    public static final String UPDATE_DATA = "update";
    public static final String CHECK_CREDENTIAL_DELETE = "credentialDelete";
    public static final String DELETE_DATA = "delete";
    public static final String CHECK_ID = "checkID";
    public static final String GET_DATA = "get";
    public static final String RUN_CHECK_DATA = "run";
    public static final String DISCOVERY_RESULT_INSERT = "discoveryDataInsert";
    public static final String MONITOR_CHECK = "monitorDataCheck";
    public static final String GET_DEFAULT_METRIC_DATA = "getDefaultMetricData";
    public static final String METRIC_CREATE = "metricCreate";
    public static final String MONITOR_DELETE = "monitorDelete";
    public static final String INSERT_POLLED_DATA = "polledDataInsert";



    //event bus address
    public static final String DATABASE_EVENTBUS_ADDRESS = "databaseAddress";
    public static final String DISCOVERY_EVENTBUS_ADDRESS = "discoveryAddress";
    public static final String METRIC_SCHEDULER_EVENTBUS_ADDRESS = "scheduleAddress";
    public static final String POLLING_EVENTBUS_ADDRESS = "pollingAddress";
    public static final String METRIC_SCHEDULER_EVENTBUS_DELETE_ADDRESS="metricDeleteScheduler";

    // SQL JSON ARRAY RESULT
    public static final String DATA = "data";
}
