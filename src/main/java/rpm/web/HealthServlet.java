package rpm.web;

import rpm.db.Db;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;

/**
 * Health endpoint:
 * - In CLOUD mode (PG env vars present): tries DB connection and reports status
 * - In LOCAL mode (no PG env vars): does NOT crash; reports DB disabled
 *
 * IMPORTANT:
 * Health servlet must NOT bootstrap DB schema. That is owned by ServerBootstrapListener.
 */
@WebServlet(urlPatterns = {"/health"})
public class HealthServlet extends HttpServlet {

    private static boolean hasPgEnv() {
        return System.getenv("PGHOST") != null
                && System.getenv("PGPORT") != null
                && System.getenv("PGDATABASE") != null
                && System.getenv("PGUSER") != null
                && System.getenv("PGPASSWORD") != null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain; charset=utf-8");

        boolean cloud = hasPgEnv();
        resp.getWriter().println("OK");
        resp.getWriter().println("mode=" + (cloud ? "CLOUD" : "LOCAL"));

        // LOCAL: do not attempt DB init/connection; keep UI runnable
        if (!cloud) {
            resp.getWriter().println("db=DISABLED (no PG env vars)");
            return;
        }

        // CLOUD: try DB connection and report
        try (Connection c = Db.getConnection()) {
            boolean connected = (c != null && !c.isClosed());
            resp.getWriter().println("db=" + (connected ? "CONNECTED" : "DISCONNECTED"));

            if (connected) {
                try (var st = c.createStatement();
                     var rs = st.executeQuery("SELECT to_regclass('public.patients'), to_regclass('public.vital_samples')")) {
                    if (rs.next()) {
                        resp.getWriter().println("patients table: " + rs.getString(1));
                        resp.getWriter().println("vital_samples table: " + rs.getString(2));
                    }
                }
            }
        } catch (Exception e) {
            // still return 200, but clearly indicate DB error
            resp.getWriter().println("db=ERROR");
            resp.getWriter().println("message=" + e.getMessage());
        }
    }
}
