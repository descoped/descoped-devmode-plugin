package io.descoped.plugins.devmode.util;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

/**
 * Created by oranheim on 07/01/2017.
 */
final public class Logger {

    public static Log INSTANCE = new SystemStreamLog();

}
