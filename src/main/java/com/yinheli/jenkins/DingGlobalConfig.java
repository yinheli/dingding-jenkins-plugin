package com.yinheli.jenkins;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yinheli
 */
@Extension
public class DingGlobalConfig extends GlobalConfiguration {

  private List<DingConfig> config = new ArrayList<>();

  public static DingGlobalConfig get() {
    return GlobalConfiguration.all().get(DingGlobalConfig.class);
  }

  public DingGlobalConfig() {
    load();
  }

  public List<DingConfig> getConfig() {
    return config;
  }

  @DataBoundSetter
  public void setConfig(List<DingConfig> config) {
    this.config = config;
  }

  @Override
  public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
    req.bindJSON(this, json);
    save();
    return super.configure(req, json);
  }
}
