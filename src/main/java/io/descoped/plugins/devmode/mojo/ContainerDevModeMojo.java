package io.descoped.plugins.devmode.mojo;

import io.descoped.plugins.devmode.util.CommonUtil;
import io.descoped.plugins.devmode.util.FileUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    @Parameter( property = "outputDirectory", required = true)
    private File outputDirectory;

    /**
     * Web content location in src
     */
    @Parameter( property = "webContent")
    private String webContent;

    /**
     * Main-class to execute
     */
    @Parameter( property = "mainClass", defaultValue = "io.descoped.container.Main")
    private String mainClass;

    @Component
    private RepositorySystem repoSystem;

    @Parameter( defaultValue = "${repositorySystemSession}", readonly = true, required = true )
    private RepositorySystemSession repoSession;

    @Parameter( defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true )
    private List<RemoteRepository> repositories;


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

    public void execute() throws MojoExecutionException {
        checkLogger();

        try {
            LOGGER.info("getBasedir: " + project.getBasedir());
            LOGGER.info("getCompileClasspathElements: " + CommonUtil.printList(project.getCompileClasspathElements()));
            LOGGER.info("getDependencies: " + CommonUtil.printList(project.getDependencies()));
//            LOGGER.info("classpath: " + classpath);

        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("", e);
        }

        /*
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your name: ");
        String username = scanner.next();
        LOGGER.info("Username: " + username);
        */

        LOGGER.info("outputDirectory: " + outputDirectory);
        LOGGER.info("webContent: " + webContent);
        LOGGER.info("mainClass: " + mainClass);

        Map<Object, Object> map = getPluginContext();
        if (map != null) {
            for (Map.Entry<Object, Object> e : map.entrySet()) {
                LOGGER.info("---> key: " + e.getKey() + " => " + e.getValue());
            }
        }

        validateOutputDirectory();

//        exec(resolveClass(mainClass));
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
