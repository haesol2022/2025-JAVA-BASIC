package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DbConnection {
    private static DbConnection dbc;

    private DbConnection() {
        try {
            DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
        } catch (SQLException ex) {
            Logger.getLogger(DbConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static DbConnection getDatabaseConnection() {
        if (dbc == null) {
            dbc = new DbConnection();
        }
        return dbc;
    }

    // 항상 새로운 커넥션을 반환!
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(
                    "jdbc:oracle:thin:@localhost:1521:XE", "C##STOREPOS", "Sollee8974!"
            );
        } catch (SQLException ex) {
            Logger.getLogger(DbConnection.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static void main(String[] args) {
        try (Connection con = DbConnection.getDatabaseConnection().getConnection()) {
            if (con != null && !con.isClosed()) {
                System.out.println("연결 성공!");
            } else {
                System.out.println("연결 실패!");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
