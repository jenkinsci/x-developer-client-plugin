package jenkins.plugins.xclient;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import org.junit.*;
import static org.junit.Assert.*;
import org.jvnet.hudson.test.RestartableJenkinsRule;

/**
 * @author Chen Jiaxing
 * @since 2020/3/30
 */
public class ConfigurationTest {

    @Rule
    public RestartableJenkinsRule rr = new RestartableJenkinsRule();

    @Test
    public void idUiAndStorage() {
        rr.then(r -> {
            assertNull("not set initially", Configuration.get().getAppId());
            HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");
            HtmlTextInput text = config.getInputByName("_.appId");
            text.setText("hello");
            r.submit(config);
            assertEquals("global config page let us edit it", "hello", Configuration.get().getAppId());
        });
        rr.then(r -> {
            assertEquals("still there after restart of Jenkins", "hello", Configuration.get().getAppId());
        });
    }

    @Test
    public void keyUiAndStorage() {
        rr.then(r -> {
            assertEquals("not set initially", "", Configuration.get().getAppKey());
            HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");
            HtmlPasswordInput text = config.getInputByName("_.appKey");
            text.setText("appKey");
            r.submit(config);
            assertEquals("global config page let us edit it", "appKey", Configuration.get().getAppKey());
        });
        rr.then(r -> {
            assertEquals("still there after restart of Jenkins.", "appKey", Configuration.get().getAppKey());
        });
    }

    @Test
    public void secretKey() {
        rr.then(r -> {
            String key = "5dbae4b5c150c63f9871ec23";
            Configuration.get().setAppKey(key);
            assertEquals(key, Configuration.get().getAppKey());
        });
    }

}
