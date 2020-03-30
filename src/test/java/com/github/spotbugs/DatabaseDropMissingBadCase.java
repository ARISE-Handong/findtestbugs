package com.github.spotbugs;

import junit.framework.TestCase;

import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseDropMissingBadCase extends TestCase {
    public void testGoodCase(Statement stmt) {
        try {
            stmt.executeUpdate("CREATE SEQUENCE something");
//            stmt.executeUpdate("DROP SEQUENCE something");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}