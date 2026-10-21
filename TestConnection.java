import java.sql.*;

public class TestConnection {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("Connected to database: " + conn.getCatalog());

            PreparedStatement ins = conn.prepareStatement("INSERT INTO test_table (name) VALUES (?)");
            ins.setString(1, "hello from java");
            ins.executeUpdate();
            System.out.println("Row inserted");

            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM test_table");
            while (rs.next()) {
                System.out.println(rs.getInt("id") + " - " + rs.getString("name"));
            }
        } catch (Exception e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }
}