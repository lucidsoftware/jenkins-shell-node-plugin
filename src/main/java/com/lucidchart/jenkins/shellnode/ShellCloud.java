package com.lucidchart.jenkins.shellnode;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

public class ShellCloud extends ShellCloudBase {
    @DataBoundConstructor
    public ShellCloud(String command, String name, String labelString) {
        super(command, name, labelString);
    }

    @Extension
    public static final class DescriptorImpl extends ShellCloudBase.Descriptor {
    }
}
