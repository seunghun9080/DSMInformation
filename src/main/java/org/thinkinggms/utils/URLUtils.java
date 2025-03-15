package org.thinkinggms.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class URLUtils {
    public static JsonObject getMealInfo(String date) {
        String content = downloadContents("https://open.neis.go.kr/hub/mealServiceDietInfo?KEY=" + FileUtils.secretResources.get("neis_key").getAsString() + "&Type=json&ATPT_OFCDC_SC_CODE=G10&SD_SCHUL_CODE=7430310&MLSV_YMD=" + date);
        return JsonParser.parseString(content).getAsJsonObject();
    }

    public static JsonObject getSchoolSchedule(String date, int limit) {
        String content = downloadContents("https://open.neis.go.kr/hub/SchoolSchedule?KEY=" + FileUtils.secretResources.get("neis_key").getAsString() + "&Type=json&pSize=" + limit + "&ATPT_OFCDC_SC_CODE=G10&SD_SCHUL_CODE=7430310&AA_FROM_YMD=" + date);
        return JsonParser.parseString(content).getAsJsonObject();
    }

    public static @NotNull String downloadContents(String urlString) {
        try {
            URL url = URI.create(urlString).toURL();
            URLConnection con = url.openConnection();
            con.setConnectTimeout(10000);
            con.connect();
            return new String(con.getInputStream().readAllBytes());
        } catch (IOException e) {
            return "";
        }
    }

    public static JsonArray parseTimeTable(JsonObject baseTimeTable, int grade, int classNum) {
        JsonArray timeTable = new JsonArray();
        JsonArray baseTime = baseTimeTable.getAsJsonArray("자료481").get(grade).getAsJsonArray().get(classNum).getAsJsonArray();
        JsonArray newTime = baseTimeTable.getAsJsonArray("자료147").get(grade).getAsJsonArray().get(classNum).getAsJsonArray();
        JsonArray subjects = baseTimeTable.getAsJsonArray("자료492");
        JsonArray teachers = baseTimeTable.getAsJsonArray("자료446");
        for (int i = 1; i < baseTime.size(); i++) {
            var a = baseTime.get(i).getAsJsonArray();
            timeTable.add(new JsonArray());
            for (int j = 1; j < a.size(); j++) {
                var identifier = Integer.toString(a.get(j).getAsInt());
                int[] divisor;
                if (identifier.length() == 4) divisor = new int[] {Integer.parseInt(identifier.substring(0, 1)), Integer.parseInt(identifier.substring(2, 4))};
                else divisor = new int[] {Integer.parseInt(identifier.substring(0, 2)), Integer.parseInt(identifier.substring(3, 5))};
                String sub = subjects.get(divisor[0]).getAsString();
                String tea = teachers.get(divisor[1]).getAsString().replace("*", "");
                JsonObject element = new JsonObject();
                element.addProperty("subject", sub);
                element.addProperty("teacher", tea);
                element.addProperty("edited", false);
                timeTable.get(i - 1).getAsJsonArray().add(element);
            }
        }
        for (int i = 1; i < newTime.size(); i++) {
            var a = newTime.get(i).getAsJsonArray();
            for (int j = 1; j < a.size(); j++) {
                var identifier = Integer.toString(a.get(j).getAsInt());
                int[] divisor;
                if (identifier.length() == 4) divisor = new int[] {Integer.parseInt(identifier.substring(0, 1)), Integer.parseInt(identifier.substring(2, 4))};
                else divisor = new int[] {Integer.parseInt(identifier.substring(0, 2)), Integer.parseInt(identifier.substring(3, 5))};
                String sub = subjects.get(divisor[0]).getAsString();
                String tea = teachers.get(divisor[1]).getAsString().replace("*", "");
                JsonObject element = new JsonObject();
                element.addProperty("subject", sub);
                element.addProperty("teacher", tea);
                element.addProperty("edited", false);
                if (timeTable.get(i - 1).getAsJsonArray().get(j - 1).getAsJsonObject().equals(element)) continue;
                element.remove("edited");
                element.addProperty("edited", true);
                timeTable.get(i - 1).getAsJsonArray().set(j - 1, element);
            }
        }
        return timeTable;
    }

    public static JsonObject baseTimeTable() {
        try {
            URL url = URI.create("http://comci.net:4082/36179?NzM2MjlfMzQ4MzJfMF8x").toURL();
            var con = url.openConnection();
            con.setConnectTimeout(1000);
            con.connect();
            return JsonParser.parseString(new String(con.getInputStream().readAllBytes(), StandardCharsets.UTF_8).split("\n")[0]).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
