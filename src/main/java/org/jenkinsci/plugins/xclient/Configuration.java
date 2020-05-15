package org.jenkinsci.plugins.xclient;

import hudson.Extension;
import hudson.Functions;
import hudson.Util;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Example of Jenkins global configuration.
 */
@Extension
public class Configuration extends GlobalConfiguration {
    private final Logger LOGGER = Logger.getLogger(Configuration.class.getName());

    /** @return the singleton instance */
    public static Configuration get() {
        return GlobalConfiguration.all().get(Configuration.class);
    }

    private String appid;
    private Secret secretKey;

    private final String serviceUrl = "https://x-developer.cn/analysis/update/";

    public Configuration() {
        load();
    }

    /** @return the currently configured label, if any */
    public String getAppid() {
        return appid;
    }
    public String getAppkey() { return Secret.toString(secretKey); }
    public String getServiceUrl() { return serviceUrl; }

    /**
     * Together with {@link #getAppid}, binds to entry in {@code config.jelly}.
     * @param appid the new value of this field
     */
    @DataBoundSetter
    public void setAppid(String appid) {
        this.appid = appid;
        save();
    }

    /**
     * Together with {@link #getAppkey}, binds to entry in {@code config.jelly}.
     * @param appkey the new value of this field
     */
    @DataBoundSetter
    public void setAppkey(String appkey) {
        this.secretKey = Secret.fromString(appkey);
        save();
    }


    public FormValidation doCheckAppid(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning(Messages.Configuration_Validation_errors_missingAppId());
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckAppkey(@QueryParameter String value) {
        if(StringUtils.isEmpty(value)) {
            return FormValidation.warning(Messages.Configuration_Validation_errors_missingAppKey());
        }
        return FormValidation.ok();
    }

    @RequirePOST
    public FormValidation doTestConnection(
            @QueryParameter String appid,
            @QueryParameter String appkey) {
        try {
            final Jenkins jenkis = Jenkins.getInstance();
            if (jenkis == null) {
                throw  new IOException("Jenkins instance is not ready");
            }

            jenkis.checkPermission(Jenkins.ADMINISTER);

            if (appid.length() == 0) {
                return FormValidation.error(Messages.Configuration_Validation_errors_missingAppId());
            }

            if (appkey.length() == 0) {
                return FormValidation.error(Messages.Configuration_Validation_errors_missingAppKey());
            }

            if (LogHttpClient.connect(serviceUrl, appid, appkey)) {
                return FormValidation.ok(Messages.Configuration_Validation_success());
            }
            return FormValidation.error(Messages.Configuration_Validation_errors_authFailed());
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
            return FormValidation.errorWithMarkup("<p>" + Messages.Configuration_Validation_failed()+"</p><pre>"+ Util.escape(Functions.printThrowable(e))+"</pre>");
        }
    }

}
