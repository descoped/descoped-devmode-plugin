package io.descoped.plugins.devmode.test;

import io.descoped.plugins.devmode.mojo.DevModeMojo;
import io.descoped.plugins.devmode.mojo.GitHubFactory;
import io.descoped.plugins.devmode.util.Logger;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by oranheim on 07/01/2017.
 */
public class DevModeMojoTest extends AbstractMojoTestCase {

    private final Log LOGGER = Logger.INSTANCE;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testMojoGoal() throws Exception {
        try {
            File testPom = new File(getBasedir(), "src/test/resources/test-pom.xml");
            DevModeMojo mojo = (DevModeMojo) lookupMojo("run", testPom);
            GitHubFactory.setMockInstance(MockHelper.mockGitHubReleases());
            Map map = new HashMap();
            map.put("project", new MojoMavenProjectStub());
            mojo.setPluginContext(map);
            mojo.execute();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
