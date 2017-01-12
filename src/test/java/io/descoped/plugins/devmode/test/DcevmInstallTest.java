package io.descoped.plugins.devmode.test;

import com.github.kevinsawicki.http.HttpRequest;
import io.descoped.plugins.devmode.util.CommonUtil;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * Created by oranheim on 12/01/2017.
 */
public class DcevmInstallTest {

    private static final Log log = new SystemStreamLog();

    private String loadTemplate(String resourceName) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream in = cl.getResourceAsStream(resourceName);
        OutputStream out = CommonUtil.newOutputStream();
        CommonUtil.writeInputToOutputStream(in, out);
        return out.toString();
    }

    private void batchAddComments(StringBuffer bash) throws IOException {
        String txt = loadTemplate("dcevm-install.txt");
        txt.replace("@JAVA_HOME", System.getProperty("java.home"));
        bash.append(txt);
    }

//    @Test
    public void testMessage() throws Exception {
        StringBuffer buf = new StringBuffer();
        batchAddComments(buf);
        log.info(buf.toString());
    }

//    @Test
    public void testDcevmLatestRelease() throws Exception {
        HttpRequest req = HttpRequest.get("https://api.github.com/repos/dcevm/dcevm/releases/latest");
        String body = req.body();
        JSONObject json = new JSONObject(body);
//        log.info("GET dcevm/release/latest: " + json.toString(2));
        log.info("..tag_name: " + json.getString("tag_name"));
        log.info("..browser_download_url: " + ((JSONObject)json.getJSONArray("assets").get(0)).getString("browser_download_url"));
    }

//    @Test
    public void testDcevmReleases() throws Exception {
        HttpRequest req = HttpRequest.get("https://api.github.com/repos/dcevm/dcevm/releases");
        String body = req.body();
        JSONArray array = new JSONArray(body);
        for (int i = 0; i < array.length(); i++) {
            JSONObject json = (JSONObject) array.get(i);
            if (json.getString("tag_name").contains("full-")) continue;
            String browserUrl = ((JSONObject) json.getJSONArray("assets").get(0)).getString("browser_download_url");
            if (!browserUrl.contains("-installer.jar")) continue;
            log.info("..." + json.getString("tag_name"));
            log.info("..browser_download_url: " + browserUrl);
        }
    }

//    @Test
    public void testHotswapLatestRelease() throws Exception {
        HttpRequest req = HttpRequest.get("https://api.github.com/repos/HotswapProjects/HotswapAgent/releases/latest");
        String body = req.body();
        JSONObject json = new JSONObject(body);
//        log.info("GET HotswapAgent/release/latest: " + json.toString(2));
        log.info("..tag_name: " + json.getString("tag_name"));
        String browserUrl = null;
        Iterator<Object> assets = json.getJSONArray("assets").iterator();
        while (assets.hasNext()) {
            JSONObject asset = (JSONObject) assets.next();
            String dlurl = asset.getString("browser_download_url");
            if (dlurl.contains("-sources")) continue;
            if (dlurl.contains("hotswap-agent") && dlurl.contains(".jar")) {
                browserUrl = asset.getString("browser_download_url");
            }
        }
        log.info("..browser_download_url: " + browserUrl);
    }

    @Test
    public void testHotswapReleases() throws Exception {
        HttpRequest req = HttpRequest.get("https://api.github.com/repos/HotswapProjects/HotswapAgent/releases");
        String body = req.body();
        JSONArray array = new JSONArray(body);
        for (int i = 0; i < array.length(); i++) {
            JSONObject json = (JSONObject) array.get(i);
//            log.info("..." + json.toString(2));
            String browserUrl = null;
            Iterator<Object> assets = json.getJSONArray("assets").iterator();
            while (assets.hasNext()) {
                JSONObject asset = (JSONObject) assets.next();
                String dlurl = asset.getString("browser_download_url");
                if (dlurl.contains("-sources")) continue;
                if (dlurl.contains("hotswap-agent") && dlurl.contains(".jar")) {
                    browserUrl = asset.getString("browser_download_url");
                }
            }
            if (browserUrl == null) continue;
            log.info("..browser_download_url: " + browserUrl);
        }
    }

}


