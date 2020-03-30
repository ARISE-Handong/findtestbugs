package com.github.spotbugs;

import junit.framework.TestCase;

import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseDropMissingGoodCase extends TestCase {
    public void testGoodCase(Statement stmt) throws SQLException{
        stmt.executeUpdate("CREATE SEQUENCE something");
        stmt.executeUpdate("DROP SEQUENCE something");
    }
}