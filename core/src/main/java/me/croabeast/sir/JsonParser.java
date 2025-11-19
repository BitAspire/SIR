package me.croabeast.sir;

import com.google.gson.*;
import lombok.experimental.UtilityClass;
import me.croabeast.file.Configurable;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class JsonParser {

    private final Gson GSON = new Gson();

    Object convertElement(JsonElement element) {
        if (element.isJsonNull()) return null;

        if (element.isJsonObject())
            return convertObject(element.getAsJsonObject());

        if (element.isJsonArray())
            return convertArray(element.getAsJsonArray());

        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean())
                return element.getAsBoolean();

            if (primitive.isNumber()) {
                final Number n = element.getAsNumber();
                return Math.floor(n.doubleValue()) == n.longValue() ?
                        n.longValue() :
                        n.doubleValue();
            }

            return element.getAsString();
        }

        return null;
    }

    Map<String, Object> convertObject(JsonObject object) {
        Map<String, Object> map = new LinkedHashMap<>();
        object.entrySet().forEach(e -> map.put(e.getKey(), convertElement(e.getValue())));
        return map;
    }

    List<Object> convertArray(JsonArray array) {
        List<Object> list = new ArrayList<>();
        array.forEach(e -> list.add(convertElement(e)));
        return list;
    }

    public Configurable fromJsonString(String json) {
        JsonElement element = GSON.fromJson(json, JsonElement.class);
        YamlConfiguration yaml = new YamlConfiguration();

        if (element.isJsonObject())
            convertObject(element.getAsJsonObject()).forEach(yaml::set);

        return Configurable.of(yaml);
    }
}
