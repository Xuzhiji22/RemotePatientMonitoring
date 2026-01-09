package rpm.web;

import rpm.db.Db;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import javax.servlet.ServletException;

import rpm.db.DbInit;



@WebServlet(urlPatterns = {"/health"})
public class HealthServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        // 只要 servlet 启动，就确保建表（幂等：IF NOT EXISTS）
        DbInit.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain; charset=utf-8");

        try (Connection c = Db.getConnection()) {
            resp.getWriter().println("OK - DB connected: " + (c != null && !c.isClosed()));
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().println("DB ERROR: " + e.getMessage());
        }
    }
}
