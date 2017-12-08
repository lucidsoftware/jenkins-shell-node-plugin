package com.lucidchart.jenkins.commandnode;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

public class CommandCloud extends ShellCloudBase {
    @DataBoundConstructor
    public CommandCloud(String command, String name, String labelString) {
        super(command, name, labelString);
    }

    @Extension
    public static final class DescriptorImpl extends ShellCloudBase.Descriptor {
    }
}
