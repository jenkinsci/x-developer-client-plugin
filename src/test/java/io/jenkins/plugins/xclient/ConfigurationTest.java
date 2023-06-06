package io.jenkins.plugins.xclient;

import org.htmlunit.html.*;
import hudson.util.Secret;
import org.junit.*;
import static org.junit.Assert.*;

import org.jvnet.hudson.test.JenkinsRule;

/**
 * @author Chen Jiaxing
 * @since 2020/3/30
 */
public class ConfigurationTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule();

    @Test
    public void idUiAndStorage() throws Exception {
        assertNull("not set initially", Configuration.get().getAppId());
        HtmlForm config = rule.createWebClient().goTo("configure").getFormByName("config");
        HtmlTextInput text = config.getInputByName("_.appId");
        text.setText("hello");
        rule.submit(config);
        assertEquals("global config page let us edit it", "hello", Configuration.get().getAppId());
    }

    @Test
    public void keyUiAndStorage() throws Exception {
        assertEquals("not set initially", "", Configuration.get().getAppKey());
        HtmlForm config = rule.createWebClient().goTo("configure").getFormByName("config");
        HtmlPasswordInput text = config.getInputByName("_.appKey");
        text.setText("appKey");
        rule.submit(config);
        assertEquals("global config page let us edit it", "appKey", Configuration.get().getAppKey());
    }

    @Test
    public void secretKey() {
        String key = "5dbae4b5c150c63f9871ec23";
        Configuration.get().setAppKey(key);
        assertEquals(key, Configuration.get().getAppKey());
    }

}
