package io.descoped.plugins.devmode.mojo;

import io.descoped.plugins.devmode.util.CommonUtil;
import io.descoped.plugins.devmode.util.FileUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Descoped Web Developer Plugin enables real time editing of source file when working on your project sources.
 *
 * todo:
 * - get classpath
 * - remove compile classes path from classpath
 * - add src directory to class path
 * - exec descoped-container with custom classpath
 */
@Mojo( name = "run", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ContainerDevModeMojo extends AbstractMojo {

    private static Log LOGGER;

    /**
     * Output directory location
     */
    @Parameter( property = "outputDirectory", defaultValue = "target/devmode")
    private File outputDirectory;

    /**
     * Web content location in src
     */
    @Parameter( property = "webContent", defaultValue = "src/main/resources/")
    private String webContent;

    /**
     * Main-class to execute
     */
    @Parameter( property = "mainClass", defaultValue = "io.descoped.container.Main")
    private String mainClass;

    /**
     * The maven project instance
     */
    @Component
    private MavenProject project;

    private void checkLogger() {
        Logger.setLOG(getLog());
        LOGGER = Logger.LOG;
        CommonUtil.printEnvVars();
    }
    
    private String getCompilePlusRuntimeClasspathJars() throws MojoExecutionException {
        try {
            StringBuffer buf = new StringBuffer();

            String currentPath = FileUtils.getCurrentPath().toString();
            buf.append(currentPath).append("/").append(webContent).append(":");
            buf.append(currentPath).append("/").append("target/classes").append(":");
            if (CommonUtil.isMojoRunningInTestingHarness()) {
                buf.append(currentPath).append("/").append("target/test-classes").append(":");
            }

            List<String> classpathElements = project.getRuntimeClasspathElements();
            if (classpathElements != null) {
                for (String e : classpathElements) {
                    if (e.endsWith(".jar")) {
                        buf.append(e);
                        if (classpathElements.indexOf(e) < classpathElements.size() - 1) {
                            buf.append(":");
                        }
                    }
                }
            }
            return buf.toString();
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error resolving classpath deps", e);
        }
    }

    public void execute() throws MojoExecutionException {
        checkLogger();

//        try {
            LOGGER.info("getBasedir: " + project.getBasedir());
//            LOGGER.info("getCompileClasspathElements: " + CommonUtil.printList(project.getCompileClasspathElements()));
            LOGGER.info("getDependencies: " + CommonUtil.printList(project.getDependencies()));

//        } catch (DependencyResolutionRequiredException e) {
//            throw new MojoExecutionException("", e);
//        }

        /*
            Build Classpath:
            1) src/main/resources/
            2) target/classes + test-classes
            3) hotswap-jar
            4) jar-files (compile classes with match for .jar)

            Jvm home: make a find . > jdk-file-snapshot.txt (before and after installation to see what has been changed)
            Java exec: install Dcevm and check that it is installed
            Java exec: add hotswap args before start
         */

        /*
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your name: ");
        String username = scanner.next();
        LOGGER.info("Username: " + username);
        */

        LOGGER.info("outputDirectory: " + outputDirectory);
        LOGGER.info("webContent: " + webContent);
//        LOGGER.info("classpath: " + getCompilePlusRuntimeClasspathJars());
        LOGGER.info("mainClass: " + mainClass);

        Map<Object, Object> map = getPluginContext();
        if (map != null) {
            for (Map.Entry<Object, Object> e : map.entrySet()) {
                LOGGER.info("---> key: " + e.getKey() + " => " + e.getValue());
            }
        }

        validateOutputDirectory();

        resolveClass(mainClass);
        exec(null);
    }

    private void exec(Class<?> mainClazz) throws MojoExecutionException {
        try {
//            LOGGER.info(mainClass.getCanonicalName());
            String separator = System.getProperty("file.separator");
            String path = System.getProperty("java.home") + separator + "bin" + separator + "java";
//            String classpath = System.getProperty("java.class.path");
            String classpath = getCompilePlusRuntimeClasspathJars();
            LOGGER.info(String.format("separator: %s -- classpath: %s -- path: %s", separator, classpath, path));

            ProcessBuilder processBuilder = new ProcessBuilder(path, "-classpath", classpath, mainClass);
            if (false) {
                StringBuffer cmd = new StringBuffer();
                for (String i : processBuilder.command()) {
                    cmd.append(i + " ");
                }
                LOGGER.info("Command:\n" + cmd);
            }
            processBuilder.directory(FileUtils.getCurrentPath().toFile());
            processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

            LOGGER.info("Starting Descoped Container in DevMode..");
            Process process = processBuilder.start();
            process.waitFor();
            LOGGER.info("Process exited with code: " + process.exitValue());
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Error starting Java process!", e);
        }
    }

    private boolean resolveClass(String className) throws MojoExecutionException {
        try {
            List<String> classpathElements = project.getRuntimeClasspathElements();

            // only for mojo testing
            if (CommonUtil.isMojoRunningInTestingHarness()) {
                if (classpathElements  == null) classpathElements = new ArrayList<>();
                classpathElements.add(FileUtils.getCurrentPath().toString() + "/target/classes:");
                classpathElements.add(FileUtils.getCurrentPath().toString() + "/target/test-classes");
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
            for(String el : classpathElements) {
                try {
                    URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{new URL("file:/" + el)});
                    classLoader.loadClass(className);
                    ok = true;
                    break;
                } catch (ClassNotFoundException e) {
                }
            }
            return ok;
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error locating class: " + className + "\nClass-path: " + System.getProperty("java.class.path"), e);
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Error locating class: " + className + "\nClass-path: " + System.getProperty("java.class.path"), e);
        }
    }

    private void validateOutputDirectory() throws MojoExecutionException {
        if (!outputDirectory.exists()) {
            Path outputPath = Paths.get(outputDirectory.getAbsolutePath());
            try {
                FileUtils.createDirectories(outputPath);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to create directory: " + outputDirectory, e);
            }

        }
    }
}
