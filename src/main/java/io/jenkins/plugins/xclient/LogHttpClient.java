package io.jenkins.plugins.xclient;

import hudson.model.TaskListener;
import net.sf.json.JSONObject;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Core logic of invoking X-Developer analysis service.
 *
 * @author Chen Jiaxing
 * @since 2020/3/30
 */
public class LogHttpClient {

    /**
     * Url that represent X-Developer analysis service.
     */
    private String serviceUrl;

    LogHttpClient() {
        this.serviceUrl = Configuration.get().getServiceUrl();
    }

    /**
     * To test connect X-Developer analysis service.
     * @return {@code true} if connect is success.
     * @throws IOException, ParseException, URISyntaxException
     */
    static boolean testConnect(String appId, String appKey) throws IOException, ParseException, URISyntaxException {
        CloseableHttpClient client = HttpClients.createDefault();
        URIBuilder builder = new URIBuilder(Configuration.get().getServiceUrl());
        List<NameValuePair> params = new LinkedList<>();
        BasicNameValuePair paramAppId = new BasicNameValuePair("appid", appId);
        BasicNameValuePair paramAppKey = new BasicNameValuePair("appkey", appKey);
        BasicNameValuePair paramTeamId = new BasicNameValuePair("team", "");
        params.add(paramAppId);
        params.add(paramAppKey);
        params.add(paramTeamId);
        builder.setParameters(params);
        HttpGet httpGet = new HttpGet(builder.build());
        CloseableHttpResponse response = client.execute(httpGet);
        String result = EntityUtils.toString(response.getEntity());
        JSONObject jsonObject = JSONObject.fromObject(result);
        if (jsonObject.getString("status").equals("team_not_found")) {
            return true;
        }
        return false;
    }

    /**
     * Try to update last commit time of repository.
     * @param params the parameters to invoke service
     * @param listener the listener of Jenkins job
     * @return {@code true} if success
     */
    boolean updateStatus(List<NameValuePair> params, TaskListener listener) {
        String result;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            URIBuilder builder = new URIBuilder(serviceUrl);
            builder.setParameters(params);
            HttpGet httpGet = new HttpGet(builder.build());
            CloseableHttpResponse response = client.execute(httpGet);
            result = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            listener.getLogger().println(String.format("Update status error: %s", e.getMessage()));
            return false;
        }
        JSONObject jsonObject = JSONObject.fromObject(result);
        String status = jsonObject.getString("status");
        if (status.equals("success")) {
            listener.getLogger().println(Messages.LogHttpClient_Response_syncSuccess());
            return analysis(jsonObject, params, listener);
        } else {
            listener.getLogger().println(status);
            return false;
        }
    }

    /**
     * Try to analysis Git log.
     * @param jsonObject the json response object
     * @param params the parameters to invoke service
     * @param listener the listener of Jenkins job
     * @return {@code true} if success
     */
    boolean analysis(JSONObject jsonObject, List<NameValuePair> params, TaskListener listener) {
        JSONObject dataSources = jsonObject.getJSONObject("dataSources");
        Map<String, File> fileMap = buildFilesParameters(dataSources);
        if (fileMap.isEmpty()) {
            return true;
        }
        listener.getLogger().println(Messages.LogHttpClient_Response_startAnalysis());
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        for (Map.Entry<String, File> entry : fileMap.entrySet()) {
            builder.addBinaryBody(entry.getKey(), entry.getValue());
        }
        for (NameValuePair param : params) {
            builder.addTextBody(param.getName(), param.getValue());
        }
        builder.addTextBody("logCount", String.valueOf(fileMap.keySet().size()));
        long timeout = 10;
        RequestConfig config = RequestConfig.custom().setConnectTimeout(Timeout.ofMinutes(timeout)).build();
        try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(config).build()) {
            HttpPost httpPost = new HttpPost(serviceUrl);
            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);
            CloseableHttpResponse response = client.execute(httpPost);
            JSONObject result = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            if (result.getString("status").equals("success")) {
                listener.getLogger().println(Messages.LogHttpClient_Response_analysisSuccess());
                return true;
            } else {
                listener.getLogger().println(result.getString("status"));
                listener.getLogger().println(result.getString("error"));
            }
        } catch (IOException | ParseException e) {
            listener.getLogger().println(String.format("Analysis error: %s", e.getMessage()));
        }
        return false;
    }

    static List<NameValuePair> buildUpdateParameters(
            String teamId,
            String logFile,
            String logFilePath,
            String lastCommit,
            boolean master,
            boolean force) {
        List<NameValuePair> params = buildBasicParameters(teamId);
        BasicNameValuePair paramLog = new BasicNameValuePair("log", logFile);
        BasicNameValuePair paramLogPath = new BasicNameValuePair("logPath", logFilePath);
        BasicNameValuePair paramLastCommit = new BasicNameValuePair("lastCommitted", lastCommit);
        BasicNameValuePair paramMaster = new BasicNameValuePair("master", String.valueOf(master));
        BasicNameValuePair paramForce = new BasicNameValuePair("force", String.valueOf(force));
        params.add(paramLog);
        params.add(paramLogPath);
        params.add(paramLastCommit);
        params.add(paramMaster);
        params.add(paramForce);
        return params;
    }

    static List<NameValuePair> buildBasicParameters(
            String teamId) {
        List<NameValuePair> params = new LinkedList<>();
        BasicNameValuePair paramAppId = new BasicNameValuePair("appid", Configuration.get().getAppId());
        BasicNameValuePair paramAppKey = new BasicNameValuePair("appkey", Configuration.get().getAppKey());
        BasicNameValuePair paramTeamId = new BasicNameValuePair("team", teamId);
        params.add(paramAppId);
        params.add(paramAppKey);
        params.add(paramTeamId);
        return params;
    }

    static Map<String, File> buildFilesParameters(JSONObject dataSources) {
        Map<String, File> fileMap = new HashMap<>();
        if (dataSources.keySet().size() > 0) {
            int index = 0;
            for (Iterator keys = dataSources.keys(); keys.hasNext(); ) {
                String key = (String) keys.next();
                String logFilePath = dataSources.getString(key);
                if (logFilePath.length() > 0) {
                    File file = new File(logFilePath);
                    if (file.exists()) {
                        key = "log" + index;
                        fileMap.put(key, file);
                        index += 1;
                    }
                }
            }
        }
        return fileMap;
    }

}
