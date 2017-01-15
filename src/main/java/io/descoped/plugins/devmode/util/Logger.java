package io.descoped.plugins.devmode.util;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

/**
 * Created by oranheim on 07/01/2017.
 */
public class Logger {

    public static Log INSTANCE = getLogger();
    private static Log LOGGER = null;

    private static Log getLogger() {
        if (LOGGER == null) {
            System.out.println("Create new SystemStreamLog instance");
            LOGGER = new SystemStreamLog();
        }
        return LOGGER;
    }

    public static void setLogger(Log logger) {
        System.out.println("Setting Mojo Logger: " + logger.getClass());
        LOGGER = logger;
    }

}
