package io.descoped.plugins.devmode.test;

import io.descoped.plugins.devmode.util.FileUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by oranheim on 08/01/2017.
 */
public class MojoMavenProjectStub extends MavenProjectStub {

    @Override
    public String getArtifactId() {
        return "io.descoped.plugins.devmode.mojo.mock";
    }

    @Override
    public String getGroupId() {
        return "project-to-test";
    }

    @Override
    public String getVersion() {
        return "1.0-SNAPSHOT";
    }

    @Override
    public File getFile() {
        return new File(FileUtils.getCurrentPath().toString());
    }

    @Override
    public List<String> getCompileClasspathElements() throws DependencyResolutionRequiredException {
        List<String> cp = new ArrayList<>();
        Path currentPath = FileUtils.getCurrentPath();
        cp.add(currentPath.resolve("target/classes").toString());
        cp.add(System.getProperty("user.home") + "/.m2/repository/com/github/kevinsawicki/http-request/6.0/http-request-6.0.jar");
        return cp;
    }
}
