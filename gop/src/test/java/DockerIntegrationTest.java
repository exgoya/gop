import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DockerIntegrationTest {
    @Test
    void mysql_mariadb_postgres_connections() throws Exception {
        Assumptions.assumeTrue(System.getenv("RUN_DOCKER_TESTS") != null, "docker tests disabled");

        assertSelectOne(
            "jdbc:mysql://127.0.0.1:3306/gop",
            "gop",
            "gop"
        );

        assertSelectOne(
            "jdbc:mariadb://127.0.0.1:3307/gop",
            "gop",
            "gop"
        );

        assertSelectOne(
            "jdbc:postgresql://127.0.0.1:5432/gop",
            "gop",
            "gop"
        );
    }

    private static void assertSelectOne(String url, String user, String password) throws Exception {
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        try (Connection con = DriverManager.getConnection(url, props);
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("select 1")) {
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }
}
