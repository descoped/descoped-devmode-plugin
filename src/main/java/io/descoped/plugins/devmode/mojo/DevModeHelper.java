package io.descoped.plugins.devmode.mojo;

import io.descoped.plugins.devmode.util.CommonUtil;
import io.descoped.plugins.devmode.util.FileUtils;
import io.descoped.plugins.devmode.util.JavaVersion;
import io.descoped.plugins.devmode.util.Logger;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by oranheim on 13/01/2017.
 */
public class DevModeHelper {

    private final Log LOGGER = Logger.INSTANCE;

    private final DevModeMojo mojo;

    public DevModeHelper(DevModeMojo devModeMojo) {
        this.mojo = devModeMojo;
    }

    public DevModeMojo getMojo() {
        return mojo;
    }

    public void init() throws MojoExecutionException {
        if (!CommonUtil.checkIfJavaExists())
            throw new MojoExecutionException("The environment variable JAVA_HOME must be set!");
        try {
            String txt = loadTemplate("mojo-config.txt");
            //txt = txt.replace("@DEVMODE", "\t\t\t" + mojo.getDevMode());
            txt = txt.replace("@BASEDIR", "\t" + mojo.getProject().getBasedir().getAbsolutePath());
            txt = txt.replace("@OUTPUT_DIRECTORY", "\t" + mojo.getOutputDirectory().getAbsolutePath());
            txt = txt.replace("@RELATIVE_OUTPUT_DIRECTORY", "\t" + relativeOutputDirectory());
            txt = txt.replace("@WEB_CONTENT_DIRECTORY", "\t" + (CommonUtil.isNotNull(mojo.getWebContent()) ? mojo.getWebContent() : "(empty)"));
            txt = txt.replace("@JAVA_VERSION", "\t\t" + JavaVersion.getVendorVersion());
            txt = txt.replace("@JAVA_HOME", "\t" + CommonUtil.getJavaHome());
            txt = txt.replace("@MAIN_CLASS", "\t\t" + mojo.getMainClass());
            BufferedReader reader = new BufferedReader(new StringReader(CommonUtil.trimRight(txt)));
            String line;
            System.out.println();
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals("")) continue;
                System.out.println(line);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error with template!", e);
        }
    }

    public String relativeOutputDirectory() {
        return mojo.getOutputDirectory().getAbsolutePath().replace(FileUtils.currentPath() + FileUtils.fileSeparator, "");
    }

    public void validateOutputDirectory() throws MojoExecutionException {
        if (!mojo.getOutputDirectory().exists()) {
            Path outputPath = Paths.get(mojo.getOutputDirectory().getAbsolutePath());
            try {
                FileUtils.createDirectories(outputPath);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to create directory: " + mojo.getOutputDirectory(), e);
            }

        }
    }

    public void validateMainClass() throws MojoExecutionException {
        if (!findClass(mojo.getMainClass())) {
            throw new MojoExecutionException("Error locating class: " + mojo.getMainClass() + "\nClass-path: " + System.getProperty("java.class.path"));
        }
    }

    private boolean isAddTestClasses() {
        String addTestClasses = System.getProperty("addTestClasses");
        return  (addTestClasses != null && "true".equals(addTestClasses));
    }

    public String getProjectClasspathJars() throws MojoExecutionException {
        try {
            StringBuffer path = new StringBuffer();

            String currentPath = FileUtils.currentPath();
            if (CommonUtil.isNotNull(mojo.getWebContent())) {
                path.append(currentPath).append("/").append(mojo.getWebContent()).append(":");
            }
            path.append(currentPath).append("/").append("target/classes").append(":");
            if (CommonUtil.isMojoRunningInTestingHarness() || CommonUtil.isMojoRunningStandalone(mojo.getProject()) || isAddTestClasses()) {
                path.append(currentPath).append("/").append("target/test-classes").append(":");
            }

            List<String> classpathElements = (isAddTestClasses() ? mojo.getProject().getTestClasspathElements() : mojo.getProject().getRuntimeClasspathElements());
            if (classpathElements != null) {
                for (String e : classpathElements) {
                    if (e.endsWith(".jar")) {
                        path.append(e);
                        if (classpathElements.indexOf(e) < classpathElements.size() - 1) {
                            path.append(":");
                        }
                    }
                }
            }
            return path.toString();
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error resolving classpath dependencies!", e);
        }
    }

    public boolean findClass(String className) throws MojoExecutionException {
        try {
            List<String> classpathElements = mojo.getProject().getRuntimeClasspathElements();

            // only for mojo testing
            if (CommonUtil.isMojoRunningInTestingHarness() || CommonUtil.isMojoRunningStandalone(mojo.getProject())) {
                if (classpathElements == null) {
                    classpathElements = new ArrayList<>();
                    classpathElements.add(0, FileUtils.currentPath() + "/target/classes");
                }
                classpathElements.add(0, FileUtils.currentPath() + "/target/test-classes");
            }

            // only for mojo testing
            if (classpathElements == null || classpathElements.isEmpty()) {
                try {
                    Class.forName(className);
                    return true;
                } catch (ClassNotFoundException e) {
                    return false;
                }
            }

            // find class on runtime classpath
            boolean ok = false;
            List<URL> urls = new ArrayList<>();
            URL[] urlArray = new URL[classpathElements.size()];
            for (String classpathElement : classpathElements) {
                File file = new File(classpathElement);
                URL url = file.toURI().toURL();
                urls.add(url);
            }

            try {
                URLClassLoader classLoader = new URLClassLoader(urls.toArray(urlArray));
                classLoader.loadClass(className);
                classLoader = null;
                ok = true;

            } catch (ClassNotFoundException e) {
            }

            return ok;
        } catch (DependencyResolutionRequiredException e) {
            return false;
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Error locating class: " + className + "\nClass-path: " + System.getProperty("java.class.path"), e);
        }
    }

    public String loadTemplate(String resourceName) throws MojoExecutionException {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            InputStream in = cl.getResourceAsStream(resourceName);
            OutputStream out = CommonUtil.newOutputStream();
            CommonUtil.writeInputToOutputStream(in, out);
            return out.toString();
        } catch (IOException e) {
            throw new MojoExecutionException("Error loading template: " + resourceName, e);
        }
    }

    public void exec(String execDirectory, List<String> args, boolean sudo, boolean waitFor, boolean printCommand) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        if (printCommand) {
            StringBuffer cmd = new StringBuffer();
            for (String i : processBuilder.command()) {
                cmd.append(i + " ");
            }
            LOGGER.debug("Command:\n" + cmd);
        }
        processBuilder.directory(Paths.get(execDirectory).toFile());
        processBuilder.inheritIO();

        LOGGER.debug("Starting process in DevMode..");
        Process process = processBuilder.start();
        LOGGER.debug("Process PID: " + CommonUtil.getPidOfProcess(process));
        if (waitFor) {
            process.waitFor();
            LOGGER.debug("Process exited with code: " + process.exitValue());
        } else {
            LOGGER.info("Exiting.. Bye!");
            System.exit(-1);
        }
    }

}
