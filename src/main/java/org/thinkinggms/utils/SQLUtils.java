package org.thinkinggms.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;
import org.thinkinggms.MessageResponseData;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SQLUtils {
    private static Connection connection;

    public static void initializeConnection() {
        initializeConnection("jdbc:mysql://localhost:3306", FileUtils.secretResources.get("sql_user_id").getAsString(), FileUtils.secretResources.get("sql_user_password").getAsString());
    }

    public static void initializeConnection(String url, String user, String password) {
        try {
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }

    public static Connection getConnection() {
        return connection;
    }

    public static void getEvents(String userId, ReplyCallbackAction action) {
        String sql = "SELECT * FROM `dsm_information`.`eventtable` WHERE userId = ?;";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, userId);
            ResultSet result = statement.executeQuery();
            while (result.next()) {
                action.addEmbeds(new EmbedBuilder().setTitle(result.getString("event_date")).setDescription(
                        "아이디 : " + result.getInt("id") + "\n" +
                        "상세 시간 : " + result.getString("event_time") + "\n" +
                                "설명 : " + result.getString("description")).build());
            }
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }

    public static int addEvent(String date, String time, String description, String userId) {
        String sql = "INSERT INTO `dsm_information`.`eventtable` (event_date, event_time, description, userId) VALUES (?, ?, ?, ?);";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, date);
            statement.setString(2, time);
            statement.setString(3, description);
            statement.setString(4, userId);
            statement.execute();
            ResultSet rs = statement.executeQuery("SELECT LAST_INSERT_ID() FROM dsm_information.eventtable");
            if (rs.next()) return rs.getInt(1);
            return -1;
        } catch (SQLException e) {
            e.printStackTrace(System.out);
            return -1;
        }
    }

    @SuppressWarnings({"deprecation", "unused"})
    public static int addEvent(@NotNull Date date, String description, String userId) {
        return addEvent((date.getYear() + 1900) + "-" + (date.getMonth() + 1) + "-" + date.getDate(), date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds(), description, userId);
    }

    public static boolean removeMessage(String inputMessage, String outputMessage, String userId) {
        String sql = "DELETE FROM `dsm_information`.`messagelog` WHERE input_message = ? AND output_message = ? AND user_id = ?;";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, inputMessage);
            statement.setString(2, outputMessage);
            statement.setString(2, userId);
            statement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace(System.out);
            return false;
        }
    }

    public static boolean removeMessage(String inputMessage, String userId) {
        String sql = "DELETE FROM `dsm_information`.`messagelog` WHERE input_message = ? AND user_id = ?;";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, inputMessage);
            statement.setString(2, userId);
            statement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace(System.out);
            return false;
        }
    }

    public static List<MessageResponseData> getMessage(String inputMessage) {
        String sql = "SELECT * FROM `dsm_information`.`messagelog` WHERE input_message=?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            List<MessageResponseData> list = new ArrayList<>();
            statement.setString(1, inputMessage);
            ResultSet result = statement.executeQuery();
            while (result.next())
                list.add(new MessageResponseData(result.getString("input_message"), result.getString("output_message"), result.getString("user_id")));
            return list;
        } catch (SQLException e) {
            e.printStackTrace(System.out);
            return new ArrayList<>();
        }
    }

    public static List<MessageResponseData> getNormalMessage(String inputMessage) {
        String sql = "SELECT * FROM `dsm_information`.`normal_message_log` WHERE input_message=?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            List<MessageResponseData> list = new ArrayList<>();
            statement.setString(1, inputMessage);
            ResultSet result = statement.executeQuery();
            while (result.next())
                list.add(new MessageResponseData(result.getString("input_message"), result.getString("output_message"), "0"));
            return list;
        } catch (SQLException e) {
            e.printStackTrace(System.out);
            return new ArrayList<>();
        }
    }

    public static List<MessageResponseData> getMessageByUser(String userId) {
        String sql = "SELECT * FROM `dsm_information`.`messagelog` WHERE user_id=? ORDER BY input_message, output_message";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            List<MessageResponseData> list = new ArrayList<>();
            statement.setString(1, userId);
            ResultSet result = statement.executeQuery();
            while (result.next()) {
                list.add(new MessageResponseData(result.getString("input_message"), result.getString("output_message"), result.getString("user_id")));
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace(System.out);
            return new ArrayList<>();
        }
    }

    public static boolean addMessage(String inputMessage, String outputMessage, String userId) {
        if (!userId.equalsIgnoreCase("419137051670347777") && !userId.equalsIgnoreCase("1285566586257674240")) {
            String testSQL = "SELECT * FROM `dsm_information`.`messagelog` WHERE input_message=? AND (user_id=? OR user_id=?);";
            try (PreparedStatement statement = getConnection().prepareStatement(testSQL)) {
                statement.setString(1, inputMessage);
                statement.setString(2, "419137051670347777");
                statement.setString(3, "1285566586257674240");
                if (statement.executeQuery().next()) return true;
            } catch (SQLException e) {
                e.printStackTrace(System.out);
            }
        }
        String sql = "INSERT INTO `dsm_information`.`messagelog` (input_message, output_message, user_id) VALUES (?, ?, ?);";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, inputMessage);
            statement.setString(2, outputMessage);
            statement.setString(3, userId);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
        return false;
    }

    public static boolean removeEvent(int id, String userId) {
        String sql = "DELETE FROM `dsm_information`.`eventtable` WHERE id = ? AND userId = ?;";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, userId);
            statement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace(System.out);
            return false;
        }
    }

    public static void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }
}
