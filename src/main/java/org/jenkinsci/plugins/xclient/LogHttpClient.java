package org.jenkinsci.plugins.xclient;

import hudson.model.TaskListener;
import net.sf.json.JSONObject;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

/**
 * @author Juatina Chen
 * @since 2020/3/30
 */
public class LogHttpClient {

    private String serviceUrl;

    public LogHttpClient(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public static boolean connect(String serviceUrl, String appId, String appKey) throws IOException {
        try (CloseableHttpClient client =HttpClients.createDefault()) {
            URIBuilder builder = new URIBuilder(serviceUrl);
            List<NameValuePair> params = buildBasicParameters(appId, appKey, "");
            builder.setParameters(params);
            HttpGet httpGet = new HttpGet(builder.build());
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                String result = EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = JSONObject.fromObject(result);
                if (jsonObject.getString("status").equals("team_not_found")) {
                    return true;
                }
            }
        } catch (ParseException | URISyntaxException e) {
            return false;
        }
        return false;
    }

    public boolean updateStatus(List<NameValuePair> params, TaskListener listener) {

        try (CloseableHttpClient client =HttpClients.createDefault()) {
            URIBuilder builder = new URIBuilder(serviceUrl);
            builder.setParameters(params);
            HttpGet httpGet = new HttpGet(builder.build());
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                String result = EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = JSONObject.fromObject(result);
                if (jsonObject.getString("status").equals("success")) {
                    listener.getLogger().println(Messages.LogHttpClient_Response_syncSuccess());
                    JSONObject dataSources = jsonObject.getJSONObject("dataSources");
                    String force = params.get(7).getValue();
                    if (dataSources.keySet().size() > 0 || Boolean.valueOf(force)) {
                        Map<String, File> fileMap = buildFilesParameters(dataSources, listener);
                        if (!fileMap.isEmpty()) {
                            listener.getLogger().println(Messages.LogHttpClient_Response_startAnalysis());
                            return analysis(fileMap, params, listener);
                        }
                    }
                    return true;
                }
            }
        } catch (IOException | ParseException | URISyntaxException e) {
            listener.getLogger().println(e.getMessage());
        }
        return false;
    }

    public boolean analysis(Map<String, File> fileMap, List<NameValuePair> params, TaskListener listener) {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        for (Map.Entry<String, File> entry : fileMap.entrySet()) {
            builder.addBinaryBody(entry.getKey(), entry.getValue());
        }
        for (NameValuePair param : params) {
            builder.addTextBody(param.getName(), param.getValue());
        }
        builder.addTextBody("logCount", String.valueOf(fileMap.keySet().size()));
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(serviceUrl);
            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                String result = EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = JSONObject.fromObject(result);
                if (jsonObject.getString("status").equals("success")) {
                    listener.getLogger().println(Messages.LogHttpClient_Response_analysisSuccess());
                    return true;
                } else {
                    listener.getLogger().println(jsonObject.getString("status"));
                    listener.getLogger().println(jsonObject.getString("message"));
                }
            }
        } catch (IOException | ParseException e) {
            listener.getLogger().println(e.getMessage());
        }
        return false;
    }

    public static List<NameValuePair> buildUpdateParameters(
            String appId,
            String appKey,
            String teamId,
            String logFile,
            String logFilePath,
            String lastCommit,
            boolean master,
            boolean force) {
        List<NameValuePair> params = buildBasicParameters(appId, appKey, teamId);
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

    public static List<NameValuePair> buildBasicParameters(
            String appId,
            String appKey,
            String teamId) {
        List<NameValuePair> params = new LinkedList<>();
        BasicNameValuePair paramAppId = new BasicNameValuePair("appid", appId);
        BasicNameValuePair paramAppKey = new BasicNameValuePair("appkey", appKey);
        BasicNameValuePair paramTeamId = new BasicNameValuePair("team", teamId);
        params.add(paramAppId);
        params.add(paramAppKey);
        params.add(paramTeamId);
        return params;
    }

    public static Map<String, File> buildFilesParameters(JSONObject dataSources, TaskListener listener) {
        Map<String, File> fileMap = new HashMap<>();
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
        return fileMap;
    }

}
