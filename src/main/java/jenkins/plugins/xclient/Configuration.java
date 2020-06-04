package jenkins.plugins.xclient;

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
 * Config X-Developer account ID-key in Jenkins.
 *
 * <p>
 *     This is a global configuration of Jenkins
 * </p>
 * @author Chen Jiaxing
 * @since 2020/3/30
 */
@Extension
public class Configuration extends GlobalConfiguration {
    private final Logger LOGGER = Logger.getLogger(Configuration.class.getName());

    /** @return the singleton instance */
    public static Configuration get() {
        return GlobalConfiguration.all().get(Configuration.class);
    }

    /**
     * Unique id that represent the given user.
     */
    private String appId;
    /**
     * Secret key that store the given user's access key.
     */
    private Secret secretKey;

    /**
     * Url tha represent X-Developer analysis service.
     */
    private final String serviceUrl = "https://x-developer.cn/analysis/update/";

    public Configuration() {
        load();
    }

    /** @return the currently configured label, if any */
    public String getAppId() {
        return appId;
    }
    public String getAppKey() { return Secret.toString(secretKey); }
    public String getServiceUrl() { return serviceUrl; }

    /**
     * Together with {@link #getAppId}, binds to entry in {@code config.jelly}.
     * @param appId the new value of this field
     */
    @DataBoundSetter
    public void setAppId(String appId) {
        this.appId = appId;
        save();
    }

    /**
     * Together with {@link #getAppKey}, binds to entry in {@code config.jelly}.
     * @param appKey the new value of this field
     */
    @DataBoundSetter
    public void setAppKey(String appKey) {
        this.secretKey = Secret.fromString(appKey);
        save();
    }


    public FormValidation doCheckAppId(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning(Messages.Configuration_Validation_errors_missingAppId());
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckAppKey(@QueryParameter String value) {
        if(StringUtils.isEmpty(value)) {
            return FormValidation.warning(Messages.Configuration_Validation_errors_missingAppKey());
        }
        return FormValidation.ok();
    }

    @RequirePOST
    public FormValidation doTestConnection(
            @QueryParameter String appId,
            @QueryParameter String appKey) {
        try {
            final Jenkins jenkis = Jenkins.getInstance();
            if (jenkis == null) {
                throw  new IOException("Jenkins instance is not ready");
            }

            jenkis.checkPermission(Jenkins.ADMINISTER);

            if (appId.length() == 0) {
                return FormValidation.error(Messages.Configuration_Validation_errors_missingAppId());
            }

            if (appKey.length() == 0) {
                return FormValidation.error(Messages.Configuration_Validation_errors_missingAppKey());
            }

            if (LogHttpClient.testConnect(appId, appKey)) {
                return FormValidation.ok(Messages.Configuration_Validation_success());
            }
            return FormValidation.error(Messages.Configuration_Validation_errors_authFailed());
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
            return FormValidation.errorWithMarkup("<p>" + Messages.Configuration_Validation_failed() +"</p><pre>"+ Util.escape(Functions.printThrowable(e))+"</pre>");
        }
    }

}
