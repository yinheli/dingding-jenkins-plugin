package com.yinheli.jenkins;

import com.alibaba.fastjson.JSONObject;
import hudson.*;
import hudson.model.*;
import hudson.scm.ChangeLogSet;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author yinheli
 */
public class DingDingNotifier extends Recorder {

  private static final Logger logger = LoggerFactory.getLogger(DingDingNotifier.class);
  private static final String apiUrl = "https://oapi.dingtalk.com/robot/send?access_token=";

  private List<DingJobConfig> config;

  @DataBoundConstructor
  public DingDingNotifier(List<DingJobConfig> config) {
    this.config = config;
  }

  public List<DingJobConfig> getConfig() {
    return config;
  }

  @DataBoundSetter
  public void setConfig(List<DingJobConfig> config) {
    this.config = config;
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                         BuildListener listener) throws InterruptedException, IOException {
    if (config == null || config.isEmpty()) {
      return true;
    }

    Jenkins jenkins = Jenkins.getInstance();

    EnvVars envVars = build.getEnvironment(listener).overrideAll(build.getBuildVariables());

    Result result = build.getResult();


    String title = String.format("# %s", build.getProject().getName());
    String duration = String.format("> ###### 构建用时 %s", build.getDurationString());

    String buildOn = build.getBuiltOnStr();
    if (StringUtils.isEmpty(buildOn)) {
      buildOn = "master";
    }
    String node = String.format("> ###### 运行于 %s", buildOn);

    String causes = getCauses(build.getCauses());
    String formatedResult = getFormatedResult(result);
    String status = String.format("> ###### 状态 %s %s [%s](%s%s)",
      formatedResult, causes, build.getDisplayName(), jenkins.getRootUrl(), build.getUrl());

    String changeLog = getChangeLog(build.getChangeSet(), build.getWorkspace());


    String messageTitle = String.format("%s 构建 %s", build.getFullDisplayName(), formatedResult);

    for (DingJobConfig c : config) {
      StringBuilder message = new StringBuilder();
      boolean send = false;

      message.append(title).append("\n");
      message.append(status).append("\n");
      message.append(duration).append("\n");
      message.append(node).append("\n\n");

      String customerMessage = StringUtils.trimToNull(c.getCustomMessage());
      if (customerMessage != null) {
        customerMessage = envVars.expand(customerMessage);
        message.append(customerMessage).append("\n\n");
      }

      message.append(changeLog);

      if (result == Result.SUCCESS && c.getOnSuccess()) {
        send = true;
      }

      if (result == Result.FAILURE && c.getOnFailed()) {
        send = true;
      }

      if (result == Result.ABORTED && c.getOnAbort()) {
        send = true;
      }

      if (send) {
        sendMessage(c.getName(), messageTitle, message.toString());
      }
    }

    return true;
  }

  private String getChangeLog(ChangeLogSet<? extends ChangeLogSet.Entry> changeSet, FilePath workspace) {
    StringBuilder sb = new StringBuilder();

    if (!changeSet.isEmptySet()) {
      sb.append("### 本次构建变更").append("\n\n");
      for (ChangeLogSet.Entry entry : changeSet) {
        String message = entry.getMsg();
        message = StringUtils.trimToEmpty(message);
        if (StringUtils.isEmpty(message)) {
          continue;
        }
        int newLineIndex = message.indexOf('\n');
        if (newLineIndex != -1) {
          message = message.substring(0, newLineIndex);
        }

        sb.append("* ").append(message).append("\n");
      }
    } else {
      RemoteCallable task = new RemoteCallable(workspace.getRemote());
      try {
        String result = workspace.act(task);
        sb.append(result);
      } catch (Exception e) {
        logger.error("remote call get git log fail", e);
      }
    }
    return sb.toString();
  }

  private String getFormatedResult(Result result) {
    if (result == Result.SUCCESS) {
      return "成功 \uD83D\uDE04 \uD83C\uDF89";
    } else if (result == Result.FAILURE) {
      return "失败 \uD83D\uDE2D";
    } else if (result == Result.ABORTED) {
      return "取消 \uD83D\uDE1D";
    }

    return result.toString();
  }

  private String getCauses(List<Cause> causes) {
    return causes.stream().map(Cause::getShortDescription).collect(Collectors.joining(", "));
  }

  private void sendMessage(String name, String title, String message) {
    DingConfig cfg = null;

    for (DingConfig c : DingGlobalConfig.get().getConfig()) {
      if (Objects.equals(name, c.getName())) {
        cfg = c;
        break;
      }
    }

    if (cfg == null) {
      logger.warn("config {} not found", name);
      return;
    }

    PostMethod post = new PostMethod(apiUrl + cfg.getAccessToken());

    JSONObject body = new JSONObject();
    body.put("msgtype", "markdown");

    JSONObject markdown = new JSONObject();
    markdown.put("title", title);
    markdown.put("text", message);

    body.put("markdown", markdown);

    JSONObject at = new JSONObject();
    at.put("isAtAll", true);
    body.put("at", at);

    try {
      post.setRequestEntity(new StringRequestEntity(body.toJSONString(), "application/json", "UTF-8"));
      HttpClient client = getHttpClient();
      client.executeMethod(post);
    } catch (Throwable e) {
      logger.error("build request error", e);
    } finally {
      post.releaseConnection();
    }
  }

  private HttpClient getHttpClient() {
    HttpClient client = new HttpClient();
    Jenkins jenkins = Jenkins.getInstance();
    if (jenkins.proxy != null) {
      ProxyConfiguration proxy = jenkins.proxy;
      if (proxy != null && client.getHostConfiguration() != null) {
        client.getHostConfiguration().setProxy(proxy.name, proxy.port);
        String username = proxy.getUserName();
        String password = proxy.getPassword();
        if (username != null && !"".equals(username.trim())) {
          logger.info("Using proxy authentication (user=" + username + ")");
          client.getState().setProxyCredentials(AuthScope.ANY,
            new UsernamePasswordCredentials(username, password));
        }
      }
    }
    return client;
  }

  @Extension
  public static class DingDingNotifierDescriptor extends BuildStepDescriptor<Publisher> {
    @Nonnull
    @Override
    public String getDisplayName() {
      return "钉钉通知";
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }
  }

  private static class RemoteCallable extends MasterToSlaveCallable<String, IOException> {
    private String workingdir;

    RemoteCallable(String workingdir) {
      this.workingdir = workingdir;
    }

    @Override
    public String call() throws IOException {
      try {
        StringBuilder sb = new StringBuilder();
        String cmd = "git log --oneline --no-merges --pretty=format:%s -10";
        Process process = Runtime.getRuntime().exec(cmd, null, new File(workingdir));
        int exitValue = process.waitFor();
        logger.info("exitValue:{}", exitValue);
        if (exitValue == 0) {
          List<String> changes = IOUtils.readLines(process.getInputStream(), "UTF-8");
          if (!changes.isEmpty()) {
            sb.append("### 最近 10 次提交").append("\n\n");
            for (String it : changes) {
              sb.append("* ").append(it).append("\n");
            }
          }
          return sb.toString();
        }
      } catch (Exception e) {
        logger.error("get git log fail", e);
      }
      return null;
    }
  }
}
