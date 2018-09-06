package com.yinheli.jenkins;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author yinheli
 */
public class DingJobConfig extends AbstractDescribableImpl<DingJobConfig> {
  private String name;
  private String customMessage;
  private boolean onSuccess = true;
  private boolean onFailed = false;
  private boolean onAbort = false;

  @DataBoundConstructor
  public DingJobConfig(String name, String customMessage, boolean onSuccess, boolean onFailed, boolean onAbort) {
    this.name = name;
    this.customMessage = customMessage;
    this.onSuccess = onSuccess;
    this.onFailed = onFailed;
    this.onAbort = onAbort;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCustomMessage() {
    return customMessage;
  }

  public void setCustomMessage(String customMessage) {
    this.customMessage = customMessage;
  }

  public boolean getOnSuccess() {
    return onSuccess;
  }

  public void setOnSuccess(boolean onSuccess) {
    this.onSuccess = onSuccess;
  }

  public boolean getOnFailed() {
    return onFailed;
  }

  public void setOnFailed(boolean onFailed) {
    this.onFailed = onFailed;
  }

  public boolean getOnAbort() {
    return onAbort;
  }

  public void setOnAbort(boolean onAbort) {
    this.onAbort = onAbort;
  }

  @Override
  public String toString() {
    return "DingJobConfig{" +
      "name='" + name + '\'' +
      ", customMessage='" + customMessage + '\'' +
      ", onSuccess=" + onSuccess +
      ", onFailed=" + onFailed +
      ", onAbort=" + onAbort +
      '}';
  }

  @Extension
  public static final class DescriptorImpl extends Descriptor<DingJobConfig> {

  }
}
