package app.ccls.packlodge;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.net.InetSocketAddress;

public class HTTPServer {

    private final Main plugin;
    private HttpServer server;

    public HTTPServer(Main plugin) {
        this.plugin = plugin;
    }

    public void startServer() throws IOException {
        int port = plugin.getConfig().getInt("web-server-port", 8798);
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/prints", new FileHandler());
        server.setExecutor(null);
        server.start();
    }

    public void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestedFile = exchange.getRequestURI().getPath().replace("/prints/", "");
            File file = new File(plugin.getDataFolder().getParentFile(), "packlodge-system/prints/" + requestedFile);

            if (file.exists() && !file.isDirectory()) {
                exchange.sendResponseHeaders(200, file.length());
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                     OutputStream os = exchange.getResponseBody()) {
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = bis.read(buffer)) != -1) {
                        os.write(buffer, 0, count);
                    }
                }
            } else {
                String response = "File not found.";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }
}
