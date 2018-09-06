package com.yinheli.jenkins;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * @author yinheli
 */
public class DingConfig extends AbstractDescribableImpl<DingConfig> {
  private String name;
  private String accessToken;

  @DataBoundConstructor
  public DingConfig(String name, String accessToken) {
    this.name = name;
    this.accessToken = accessToken;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  @Override
  public String toString() {
    return "DingConfig{" +
      "name='" + name + '\'' +
      ", accessToken='" + accessToken + '\'' +
      '}';
  }

  @Extension
  public static final class DescriptorImpl extends Descriptor<DingConfig> {
    public FormValidation doCheckName(@QueryParameter String name) {
      if (StringUtils.isEmpty(name)) {
        return FormValidation.error("名称和 accessToken 不能为空");
      }
      return FormValidation.ok();
    }

    public FormValidation doCheckAccessToken(@QueryParameter String accessToken) {
      if (StringUtils.isEmpty(accessToken)) {
        return FormValidation.error("accessToken 不能为空");
      }
      return FormValidation.ok();
    }
  }
}
