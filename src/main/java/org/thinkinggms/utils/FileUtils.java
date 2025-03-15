package org.thinkinggms.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.thinkinggms.DSMInformation;

import java.io.IOException;
import java.io.InputStream;

public class FileUtils {
    public static final JsonObject secretResources = readSecret();

    public static @NotNull JsonObject readSecret() {
        try (InputStream r = DSMInformation.class.getClassLoader().getResourceAsStream("secret_resources.json")) {
            if (r == null) return new JsonObject();
            return JsonParser.parseString(new String(r.readAllBytes())).getAsJsonObject();
        } catch (IOException e) {
            return new JsonObject();
        }
    }

    public static @NotNull JsonArray getRules() {
        try (InputStream r = DSMInformation.class.getClassLoader().getResourceAsStream("fRules.json")) {
            if (r == null) return new JsonArray();
            return JsonParser.parseString(new String(r.readAllBytes())).getAsJsonArray();
        } catch (IOException e) {
            return new JsonArray();
        }
    }
}
