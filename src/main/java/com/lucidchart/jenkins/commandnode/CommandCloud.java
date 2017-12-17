package com.lucidchart.jenkins.commandnode;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;

public class CommandCloud extends ShellCloudBase {
    @DataBoundConstructor
    public CommandCloud(String command, String name, String labelString) {
        super(command, name, labelString);
    }

    public HttpResponse doProvision(@QueryParameter String cloud) {
        return super.doProvision(cloud);
    }

    @Extension
    public static final class DescriptorImpl extends ShellCloudBase.Descriptor {
    }
}
