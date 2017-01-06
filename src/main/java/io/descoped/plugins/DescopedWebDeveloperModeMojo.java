package io.descoped.plugins;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;

/**
 * Descoped Web Developer Plugin enables real time editing of source file when working on your project sources.
 *
 * @goal run
 * @phase process-classes
 */
@Mojo( name = "DescopedWebDeveloperPlugin")
public class DescopedWebDeveloperModeMojo extends AbstractMojo {

    /**
     * Location of the file.
     *
     * @parameter expression="project.build.directory"
     * @required
     */
    private File outputDirectory;

    private void exec(Class<?> mainClass) throws MojoExecutionException {
        try {
            getLog().info(mainClass.getCanonicalName());
            String separator = System.getProperty("file.separator");
            String path = System.getProperty("java.home") + separator + "bin" + separator + "java";
            String classpath = System.getProperty("java.class.path") + ":/Users/oranheim/.m2/repository/io/descoped/plugins/devmode/1.0.0-SNAPSHOT/devmode-1.0.0-SNAPSHOT.jar";
            getLog().info(String.format("separator: %s -- classpath: %s -- path: %s", separator, classpath, path));

            ProcessBuilder processBuilder = new ProcessBuilder(path, "-classpath", classpath, mainClass.getCanonicalName());
            StringBuffer cmd = new StringBuffer();
            for (String i : processBuilder.command()) {
                cmd.append(i + " ");
            }
            getLog().info("Command:\n" + cmd);
            processBuilder.directory(outputDirectory);
            processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

            Process process = processBuilder.start();
            process.waitFor();
            getLog().info("Process exited with code: " + process.exitValue());
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Error starting Java process!", e);
        }
    }

    public void execute() throws MojoExecutionException {
        getLog().info("Hello: " + outputDirectory);

        exec(Echo.class);
    }
}
