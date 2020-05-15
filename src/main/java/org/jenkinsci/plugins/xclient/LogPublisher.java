package org.jenkinsci.plugins.xclient;

import hudson.*;
import hudson.cli.CLICommand;
import hudson.model.*;
import hudson.tasks.*;
import hudson.util.ArgumentListBuilder;
import hudson.util.StreamTaskListener;
import jenkins.tasks.SimpleBuildStep;
import org.apache.hc.core5.http.NameValuePair;
import org.kohsuke.stapler.*;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Juatina Chen
 * @since 2020/3/29
 */
public class LogPublisher extends Publisher implements SimpleBuildStep {
    // Command knows not to work
    private static final List<String> commandBlacklist = Arrays.asList(
            "groovysh"
    );

    /**
     * UID string that represent team.
     */
    private String teamId;

    /**
     * If true, all the issues will be published.
     */
    private boolean master;

    /**
     * If true, it will forced analysis.
     */
    private boolean forceAnalysis;

    /**
     * Default Constructor.
     */
    @Deprecated
    public LogPublisher() {}

    public LogPublisher(String teamId) {
        this(teamId, false, false);
    }

    @DataBoundConstructor
    public LogPublisher(String teamId, boolean master, boolean forceAnalysis) {
        this.teamId = teamId;
        this.master = master;
        this.forceAnalysis = forceAnalysis;
    }


    public String getTeamId() { return this.teamId; }

    public boolean getMaster() { return this.master; }

    public boolean getForceAnalysis() { return this.forceAnalysis; }

    /**
     * Together with {@link #getTeamId}, binds to entry in {@code config.jelly}.
     * @param teamId UID string that represent team.
     */
    @DataBoundSetter
    public void setTeamId(String teamId) { this.teamId = teamId; }

    /**
     * Together with {@link #getMaster}, binds to entry in {@code config.jelly}.
     * @param master If true, all the issues will be published.
     */
    @DataBoundSetter
    public void setMaster(boolean master) { this.master = master; }

    /**
     * Together with {@link #getForceAnalysis} , binds to entry in {@code config.jelly}.
     * @param forceAnalysis If true, it will forced analysis.
     */
    @DataBoundSetter
    public void setForceAnalysis(boolean forceAnalysis) { this.forceAnalysis = forceAnalysis; }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        Configuration config = Configuration.get();
        String logFile = workspace.getName() + ".csv";
        String logFilePath = workspace.getRemote() + "/" + logFile;
        File file = new File(logFilePath);
        if (file.exists()) {
            boolean result = file.delete();
            if (result) {
                listener.getLogger().println(Messages.LogPublisher_Git_logDeleted());
            } else {
                listener.getLogger().println(Messages.LogPublisher_Git_errors_logDeletedFailed());
            }
        }
        String command = "git log --pretty=format:%an,%ae,%ai,%s";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamTaskListener taskListener = new StreamTaskListener(out);
        ArgumentListBuilder builder = new ArgumentListBuilder();
        builder.addTokenized(command);
        Launcher.ProcStarter starter = launcher.launch();
        starter = starter.cmds(builder).stdout(taskListener);
        starter = starter.pwd(workspace).envs(build.getEnvironment(taskListener));
        Proc proc = launcher.launch(starter);
        proc.join();
        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream(logFilePath);
            fileOut.write(out.toByteArray());
        } catch (FileNotFoundException e) {
            listener.getLogger().println(e.getMessage());
        } catch (IOException e) {
            listener.getLogger().println(e.getMessage());
        } finally {
            if (fileOut != null) {
                fileOut.close();
            }
        }
        listener.getLogger().println(Messages.LogPublisher_Git_logGenerated());
        final String commandLine = "git log --pretty=format:\"%at\" -1";
        String lastCommit;
        out = new ByteArrayOutputStream();
        taskListener = new StreamTaskListener(out);
        builder = new ArgumentListBuilder();
        builder.addTokenized(commandLine);
        starter = launcher.launch();
        starter = starter.cmds(builder).stdout(taskListener);
        starter = starter.pwd(workspace).envs(build.getEnvironment(taskListener));
        proc = launcher.launch(starter);
        proc.join();
        lastCommit = out.toString();
        if (test) {
            lastCommit = "1585572510";
        }
        Pattern pattern = Pattern.compile("[0-9]*");
        if (lastCommit == null || !pattern.matcher(lastCommit).matches()) {
            listener.getLogger().println(Messages.LogPublisher_Git_errors_logUpdateFailed());
            if (!test) {
                build.setResult(Result.UNSTABLE);
            }
        } else {
            List<NameValuePair> params = LogHttpClient.buildUpdateParameters(
                    config.getAppid(),
                    config.getAppkey(),
                    teamId,
                    logFile,
                    logFilePath,
                    lastCommit,
                    master,
                    forceAnalysis
            );
            LogHttpClient client = new LogHttpClient(Configuration.get().getServiceUrl());
            boolean result = client.updateStatus(params, listener);

            if (result) {
                build.setResult(Result.SUCCESS);
            } else {
                listener.getLogger().println(Messages.LogHttpClient_Response_analysisFailed());
                if (!test) {
                    build.setResult(Result.UNSTABLE);
                }
            }
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.LogPublisher_DescriptorImpl_DisplayName();
        }
    }

    public static boolean test = false;
}
