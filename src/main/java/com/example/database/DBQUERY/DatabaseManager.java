    package com.example.database.DBQUERY;

    import com.example.database.request;

    import java.sql.*;
    import java.util.ArrayList;
    import java.util.List;

    public class DatabaseManager {

        //table request admin area
        public List<request> fetchRequests() {
            List<request> list = new ArrayList<>();
            String query = "SELECT * FROM requests";
            try (Connection conn = DriverManager.getConnection(Database.url, Database.user, Database.password);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    list.add(new request(
                            rs.getString("REQUEST_ID"),
                            rs.getString("EVACUATION_CENTER"),
                            rs.getString("ITEM"),
                            rs.getInt("QUANTITY"),
                            rs.getString("DATE")
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }



    }