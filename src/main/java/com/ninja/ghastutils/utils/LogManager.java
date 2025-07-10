
package com.ninja.ghastutils.utils;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.config.ConfigType;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;

public class LogManager {
    private static GhastUtils plugin;
    private static Logger logger;
    private static boolean debugEnabled = false;
    private static boolean fileLoggingEnabled = false;
    private static String logDirectory = "logs";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String ECONOMY_LOG = "economy.log";
    private static final String ERROR_LOG = "errors.log";
    private static final String DEBUG_LOG = "debug.log";
    private static final String TRANSACTION_LOG = "transactions.log";

    public static void initialize(GhastUtils pluginInstance) {
        plugin = pluginInstance;
        logger = plugin.getLogger();
        reload();
        File logDir = new File(plugin.getDataFolder(), logDirectory);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        info("LogManager initialized");
    }

    public static void reload() {
        FileConfiguration config = plugin.getConfigManager().getConfig(ConfigType.MAIN);
        debugEnabled = config.getBoolean("logging.debug-enabled", false);
        fileLoggingEnabled = config.getBoolean("logging.file-logging-enabled", true);
        logDirectory = config.getString("logging.directory", "logs");
    }

    public static void info(String message) {
        logger.info(message);
        if (fileLoggingEnabled) {
            writeToFile("debug.log", "[INFO] " + message);
        }

    }

    public static void warning(String message) {
        logger.warning(message);
        if (fileLoggingEnabled) {
            writeToFile("debug.log", "[WARNING] " + message);
        }

    }

    public static void error(String message) {
        logger.severe(message);
        if (fileLoggingEnabled) {
            writeToFile("errors.log", message);
            writeToFile("debug.log", "[ERROR] " + message);
        }

    }

    public static void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
        if (fileLoggingEnabled) {
            writeToFile("errors.log", message + "\n" + getStackTraceAsString(throwable));
            writeToFile("debug.log", "[ERROR] " + message);
        }

    }

    public static void debug(String message) {
        if (debugEnabled) {
            logger.info("[DEBUG] " + message);
            if (fileLoggingEnabled) {
                writeToFile("debug.log", "[DEBUG] " + message);
            }
        }

    }

    public static void transaction(String type, String player, double amount, String source, String result) {
        String message = String.format("[%s] %s: %.2f | Source: %s | Result: %s", type, player, amount, source, result);
        if (debugEnabled) {
            logger.info("[TRANSACTION] " + message);
        }

        if (fileLoggingEnabled) {
            writeToFile("transactions.log", message);
            writeToFile("economy.log", message);
        }

    }

    public static void custom(String logFile, String message) {
        if (fileLoggingEnabled) {
            writeToFile(logFile, message);
        }

    }

    private static String getStackTraceAsString(Throwable throwable) {
        if (throwable == null) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(throwable.toString()).append("\n");

            for(StackTraceElement element : throwable.getStackTrace()) {
                sb.append("\tat ").append(element.toString()).append("\n");
            }

            return sb.toString();
        }
    }

    private static void writeToFile(String fileName, String message) {
        if (fileLoggingEnabled && plugin != null) {
            File logDir = new File(plugin.getDataFolder(), logDirectory);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            File logFile = new File(logDir, fileName);

            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                String timestamp = formatter.format(new Date());
                writer.println("[" + timestamp + "] " + message);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to write to log file: " + fileName, e);
            }

        }
    }
}
