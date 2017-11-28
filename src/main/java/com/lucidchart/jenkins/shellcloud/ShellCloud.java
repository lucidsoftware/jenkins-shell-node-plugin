package com.lucidchart.jenkins.shellcloud;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

public class ShellCloud extends ShellCloudBase {
    @DataBoundConstructor
    public ShellCloud(String command, String name, int executorLimit, String labelString) {
        super(command, name, executorLimit, labelString);
    }

    @Extension
    public static final class DescriptorImpl extends ShellCloudBase.Descriptor {
    }
}
