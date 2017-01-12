package io.descoped.plugins.devmode.util;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by oranheim on 04/01/2017.
 */
public class CommonUtil {

    private static final Log LOGGER = new SystemStreamLog();

    private static ThreadLocal<OutputStream> outputLocal = new ThreadLocal<OutputStream>() {
        private OutputStream output = null;

        @Override
        protected OutputStream initialValue() {
            if (output == null) {
                output = newOutputStream();
            }
            return output;
        }

        @Override
        public void remove() {
            try {
                output.flush();
                output.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            super.remove();
        }
    };

    public static void closeOutputStream(OutputStream output) throws IOException {
        output.flush();
        output.close();
    }

    public static OutputStream closeAndCreateNewOutputStream(OutputStream output) throws IOException {
        closeOutputStream(output);
        return newOutputStream();
    }

    public static OutputStream getConsoleOutputStream() {
        return outputLocal.get();
    }

    public static OutputStream newOutputStream() {
        return new OutputStream() {
            private StringBuilder string = new StringBuilder();

            @Override
            public void write(int b) throws IOException {
                this.string.append((char) b);
            }

            @Override
            public synchronized void write(byte[] b, int off, int len) {
                try {
                    this.string.append(new String(b, 0, len, "UTF-8"));
                } catch (Exception e) {

                }
            }


            public String toString() {
                return this.string.toString();
            }
        };
    }

    public static OutputStream writeInputToOutputStream(InputStream in) throws IOException {
        OutputStream out = newOutputStream();
        byte[] buffer = new byte[1024];
        int len = in.read(buffer);
        while (len != -1) {
            out.write(buffer, 0, len);
            len = in.read(buffer);
        }
        out.close();
        return out;
    }

    public static OutputStream writeInputToOutputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len = in.read(buffer);
        while (len != -1) {
            out.write(buffer, 0, len);
            len = in.read(buffer);
        }
        out.close();
        return out;
    }

    public static void printStream(String msg, InputStream in, Log log) throws IOException {
        OutputStream out = CommonUtil.newOutputStream();
        CommonUtil.writeInputToOutputStream(in, out);
        log.info(msg + ": " + out);
    }
    
    public static void printEnvVars() {
        LOGGER.info("------------> Environment Varaibles <------------");
        Map<String, String> env = System.getenv();
        for(Map.Entry<String,String> e : env.entrySet()) {
            if (e.getKey().contains("CI_NEXUS")) continue;
            LOGGER.info(String.format("%s=%s", e.getKey(), e.getValue()));
        }
        LOGGER.info("------------> System Properties <------------");
        Properties props = System.getProperties();
        for(Map.Entry<Object, Object> e : props.entrySet()) {
            LOGGER.info(String.format("%s=%s", e.getKey(), e.getValue()));
        }
        LOGGER.info("------------> -o-o-o-o-o-o-o <------------");
    }

    public static boolean isMojoRunningInTestingHarness() {
        try {
            Class<?> mojo = Class.forName("org.apache.maven.plugin.testing.AbstractMojoTestCase");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isMojoRunningStandalone(MavenProject project) {
        boolean ok = Boolean.valueOf(project.getProperties().getProperty("test.mojo"));
        LOGGER.info("-------------> test.mojo: " + ok);
        return ok;
    }

    public static String printList(List<?> list) {
        if (list == null || list.isEmpty()) return "(empty)";
        StringBuffer buf = new StringBuffer();
        for(Object obj : list) {
            buf.append(obj.toString() + "\n");
        }
        return buf.toString();
    }

    public static synchronized long getPidOfProcess(Process p) {
        long pid = -1;

        try {
            if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field f = p.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                pid = f.getLong(p);
                f.setAccessible(false);
            }
        } catch (Exception e) {
            pid = -1;
        }
        return pid;
    }
}
