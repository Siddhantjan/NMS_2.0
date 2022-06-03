import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Iterator;

public class Test {
    public static void main(String[] args) {
     var js = new JsonArray();
     js.add("discovery.id");
     js.add("credential");
     js.add("port");
     js.add("ip");
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("discovery.id",1).put("credential",1).put("ip","10.20.40.178").put("pooja","sharma")
                .put("Siddhant","Jain");
        var keys = jsonObject.fieldNames();
        keys.removeIf(key -> !js.contains(key));
        System.out.println(jsonObject);

    }
}
