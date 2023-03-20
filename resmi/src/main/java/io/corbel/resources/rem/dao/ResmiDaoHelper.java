package io.corbel.resources.rem.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Alexander De Leon (alex.deleon@devialab.com)
 */
public class ResmiDaoHelper {

    static JsonArray renameIds(JsonArray array, boolean wildcard) {
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                renameIds(object, wildcard);
            }
        }
        return array;
    }

    static JsonElement renameIds(JsonObject object, boolean wildcard) {
        object.add("id", object.get(JsonRelation._DST_ID));
        object.remove(JsonRelation._DST_ID);
        if (!wildcard) {
            object.remove(JsonRelation._SRC_ID);
        }
        return object;
    }
}
