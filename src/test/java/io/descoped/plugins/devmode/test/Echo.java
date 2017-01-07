package io.descoped.plugins.devmode.test;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

/**
 * Created by oranheim on 06/01/2017.
 */
public class Echo {

    private static final Log LOGGER = new SystemStreamLog();

    public Echo() {
        LOGGER.info("Echo will wait for 2000ms..");
    }

    private void sleep() {
        try {
            LOGGER.info("Sleep some...");
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Echo echo = new Echo();
        echo.sleep();
    }

}
