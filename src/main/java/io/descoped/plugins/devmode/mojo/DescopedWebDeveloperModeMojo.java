package io.descoped.plugins.devmode.mojo;

import io.descoped.plugins.devmode.util.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Descoped Web Developer Plugin enables real time editing of source file when working on your project sources.
 *
 * @goal run
 * @phase process-classes
 */
@Mojo( name = "DescopedWebDeveloperPlugin")
public class DescopedWebDeveloperModeMojo extends AbstractMojo {

    private static Log LOGGER;

    /**
     * Location of the file.
     *
     * @parameter expression="project.build.directory"
     * @required
     */
    private File outputDirectory;

    private void checkLogger() {
        Logger.setLOG(getLog());
        LOGGER = Logger.LOG;
    }

    public void execute() throws MojoExecutionException {
        checkLogger();

        LOGGER.info("Hello: " + outputDirectory);

        validateOutputDirectory();

        exec(resolveClass("io.descoped.plugins.devmode.test.Echo"));
    }

    private void exec(Class<?> mainClass) throws MojoExecutionException {
        try {
            LOGGER.info(mainClass.getCanonicalName());
            String separator = System.getProperty("file.separator");
            String path = System.getProperty("java.home") + separator + "bin" + separator + "java";
            String classpath = System.getProperty("java.class.path");
            LOGGER.info(String.format("separator: %s -- classpath: %s -- path: %s", separator, classpath, path));

            ProcessBuilder processBuilder = new ProcessBuilder(path, "-classpath", classpath, mainClass.getCanonicalName());
            if (false) {
                StringBuffer cmd = new StringBuffer();
                for (String i : processBuilder.command()) {
                    cmd.append(i + " ");
                }
                LOGGER.info("Command:\n" + cmd);
            }
            processBuilder.directory(outputDirectory);
            processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

            Process process = processBuilder.start();
            process.waitFor();
            LOGGER.info("Process exited with code: " + process.exitValue());
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Error starting Java process!", e);
        }
    }

    private Class<?> resolveClass(String className) throws MojoExecutionException {
        try {
            Class<?> clazz = Class.forName(className);
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Error locating class: " + className, e);
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
