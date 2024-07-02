package app.ccls.packlodge;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.logging.Logger;

public class HTTPServer {

    private final Main plugin;
    private final Logger logger;
    private HttpServer server;
    private String storedUsername;
    private String storedPassword;
    private boolean authenticationEnabled;

    public HTTPServer(Main plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.authenticationEnabled = plugin.getConfig().getBoolean("web-server-authentication", true);
        createDirectories();
        if (authenticationEnabled) {
            loadCredentials();
        }
    }

    private void loadCredentials() {
        File passwordFile = new File(plugin.getDataFolder().getParentFile(), "packlodge-system/web-server/credentials.properties");
        if (!passwordFile.exists()) {
            copyDefaultPasswordFile(passwordFile);
        }

        if (passwordFile.exists()) {
            try (InputStream is = new FileInputStream(passwordFile)) {
                Properties properties = new Properties();
                properties.load(is);
                storedUsername = properties.getProperty("username", "admin");
                storedPassword = properties.getProperty("password", "admin");
                logger.info("Loaded credentials: " + storedUsername + "/" + storedPassword);
            } catch (IOException e) {
                e.printStackTrace();
                storedUsername = "admin"; // Set default username if an error occurs while reading the file
                storedPassword = "admin"; // Set default password if an error occurs while reading the file
            }
        }
    }

    private void copyDefaultPasswordFile(File passwordFile) {
        logger.info("Attempting to copy default credentials.properties file from resources.");
        try (InputStream is = getClass().getResourceAsStream("/credentials.properties");
             FileOutputStream fos = new FileOutputStream(passwordFile)) {
            if (is != null) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
                logger.info("Successfully copied default credentials.properties file from resources.");
            } else {
                logger.warning("Resource file credentials.properties not found in resources.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startServer() throws IOException {
        createDirectories();
        int port = plugin.getConfig().getInt("web-server-port", 8798);
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/web-server/pac.html", new PacFileHandler());
        server.createContext("/web-server/prints", new FileHandler());
        server.setExecutor(null);
        server.start();
    }

    private void createDirectories() {
        File webServerDir = new File(plugin.getDataFolder().getParentFile(), "packlodge-system/web-server");
        File printsDir = new File(webServerDir, "prints");

        if (!webServerDir.exists()) {
            boolean dirCreated = webServerDir.mkdirs();
            if (dirCreated) {
                logger.info("Created directory: " + webServerDir.getAbsolutePath());
            } else {
                logger.warning("Failed to create directory: " + webServerDir.getAbsolutePath());
            }
        }

        if (!printsDir.exists()) {
            boolean dirCreated = printsDir.mkdirs();
            if (dirCreated) {
                logger.info("Created directory: " + printsDir.getAbsolutePath());
            } else {
                logger.warning("Failed to create directory: " + printsDir.getAbsolutePath());
            }
        }
    }

    public void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private class PacFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (authenticationEnabled && !isAuthenticated(exchange)) {
                sendUnauthorizedResponse(exchange);
                return;
            }

            File file = new File(plugin.getDataFolder().getParentFile(), "packlodge-system/web-server/pac.html");

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

        private boolean isAuthenticated(HttpExchange exchange) {
            if (storedUsername == null || storedPassword == null) {
                return false;
            }

            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Basic")) {
                String base64Credentials = authHeader.substring("Basic".length()).trim();
                String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                if (values.length == 2) {
                    String username = values[0];
                    String password = values[1];
                    return storedUsername.equals(username) && storedPassword.equals(password);
                }
            }
            return false;
        }

        private void sendUnauthorizedResponse(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"Packlodge\"");
            String response = "Unauthorized";
            exchange.sendResponseHeaders(401, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    private class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (authenticationEnabled && !isAuthenticated(exchange)) {
                sendUnauthorizedResponse(exchange);
                return;
            }

            String requestedFile = exchange.getRequestURI().getPath().replace("/web-server/prints/", "");
            File file = new File(plugin.getDataFolder().getParentFile(), "packlodge-system/web-server/prints/" + requestedFile);

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

        private boolean isAuthenticated(HttpExchange exchange) {
            if (storedUsername == null || storedPassword == null) {
                return false;
            }

            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Basic")) {
                String base64Credentials = authHeader.substring("Basic".length()).trim();
                String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                if (values.length == 2) {
                    String username = values[0];
                    String password = values[1];
                    return storedUsername.equals(username) && storedPassword.equals(password);
                }
            }
            return false;
        }

        private void sendUnauthorizedResponse(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"Packlodge\"");
            String response = "Unauthorized";
            exchange.sendResponseHeaders(401, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}
