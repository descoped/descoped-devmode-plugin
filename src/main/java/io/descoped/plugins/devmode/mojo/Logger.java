package io.descoped.plugins.devmode.mojo;

import org.apache.maven.plugin.logging.Log;

/**
 * Created by oranheim on 07/01/2017.
 */
final public class Logger {

    public static Log LOG = null;

    protected static void setLOG(Log logger) {
        LOG = logger;
    }
}
