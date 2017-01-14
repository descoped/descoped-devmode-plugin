package io.descoped.plugins.devmode.util;

/**
 * Created by oranheim on 08/01/2017.
 */
public class JavaVersion {

    private static final String version;
    private static final String major;
    private static final Integer minor;
    private static final String build;

    static  {
        version = System.getProperty("java.runtime.version");
        int verIndex = version.indexOf("_");
        major = version.substring(0, verIndex);
        int majorIndex = version.indexOf("-");
        minor = Integer.valueOf(version.substring(verIndex+1, majorIndex));
        build = version.substring(majorIndex+1);
    }

    public static String getVersion() {
        return version;
    }

    public static String getMajor() {
        return major;
    }

    public static Integer getMinor() {
        return minor;
    }

    public static String getBuild() {
        return build;
    }

    public static boolean isJdk7() {
        return major.startsWith("1.7");
    }

    public static boolean isJdk8() {
        return major.startsWith("1.8");
    }

    public static CharSequence getVersionInfo() {
        return String.format("%s-u%s_%s", (isJdk8() ? "8" : "7"), getMinor(), getBuild());
    }

    public static String getVendorVersion() {
        return String.format("%s (%s) v%s", System.getProperty("java.vm.vendor"), System.getProperty("java.runtime.name"), getVersionInfo());
    }
}
