package io.descoped.plugins.devmode.mojo;

import io.descoped.plugins.devmode.util.CommonUtil;
import io.descoped.plugins.devmode.util.FileUtils;
import io.descoped.plugins.devmode.util.Logger;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    private static final Log LOGGER = Logger.INSTANCE;

    private final MavenProject project;
    private final File outputDirectory;
    private final String webContent;
    private final String mainClass;

    public DevModeHelper(MavenProject project, File outputDirectory, String webContent, String mainClass) {
        this.project = project;
        this.outputDirectory = outputDirectory;
        this.webContent = webContent;
        this.mainClass = mainClass;
    }

    public void init() {
        StringBuffer initMsg = new StringBuffer();
        initMsg.append("Configuration:\n");
        initMsg.append("\t--o  ").append("Project BasedDir: ").append(project.getBasedir()).append("\n");
        initMsg.append("\t--o  ").append("Web Content directory: ").append(webContent).append("\n");
        initMsg.append("\t--o  ").append("Main-Class: ").append(mainClass).append("\n");
        LOGGER.info(initMsg);
    }

    public void validateOutputDirectory() throws MojoExecutionException {
        if (!outputDirectory.exists()) {
            Path outputPath = Paths.get(outputDirectory.getAbsolutePath());
            try {
                FileUtils.createDirectories(outputPath);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to create directory: " + outputDirectory, e);
            }

        }
    }

    public String getCompilePlusRuntimeClasspathJars() throws MojoExecutionException {
        try {
            StringBuffer path = new StringBuffer();

            String currentPath = FileUtils.getCurrentPath().toString();
            path.append(currentPath).append("/").append(webContent).append(":");
            path.append(currentPath).append("/").append("target/classes").append(":");
            if (CommonUtil.isMojoRunningInTestingHarness() || CommonUtil.isMojoRunningStandalone(project)) {
                path.append(currentPath).append("/").append("target/test-classes").append(":");
            }

            List<String> classpathElements = project.getRuntimeClasspathElements();
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
            throw new MojoExecutionException("Error resolving classpath deps", e);
        }
    }

    public boolean findClass(String className) throws MojoExecutionException {
        try {
            List<String> classpathElements = project.getRuntimeClasspathElements();

            // only for mojo testing
            if (CommonUtil.isMojoRunningInTestingHarness() || CommonUtil.isMojoRunningStandalone(project)) {
                if (classpathElements == null) {
                    classpathElements = new ArrayList<>();
                    classpathElements.add(0, FileUtils.getCurrentPath().toString() + "/target/classes");
                }
                classpathElements.add(0, FileUtils.getCurrentPath().toString() + "/target/test-classes");
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

    public String getJavaHomeExecutable() {
        String separator = System.getProperty("file.separator");
        String path = System.getProperty("java.home") + separator + "bin" + separator + "java";
        return path;
    }

    public String loadTemplate(String resourceName) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream in = cl.getResourceAsStream(resourceName);
        OutputStream out = CommonUtil.newOutputStream();
        CommonUtil.writeInputToOutputStream(in, out);
        return out.toString();
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
        processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        processBuilder.inheritIO();

        LOGGER.info("Starting process in DevMode..");
        Process process = processBuilder.start();
        LOGGER.info("Process PID: " + CommonUtil.getPidOfProcess(process));
        if (waitFor) {
            process.waitFor();
            LOGGER.info("Process exited with code: " + process.exitValue());
        } else {
            LOGGER.info("Leaving process in background. Bye!");
            System.exit(-1);
        }
    }


}
