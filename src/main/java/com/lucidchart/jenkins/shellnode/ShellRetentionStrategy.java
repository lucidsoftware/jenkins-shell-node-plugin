package com.lucidchart.jenkins.shellnode;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

public class ShellRetentionStrategy extends ShellRetentionStrategyBase {
    @DataBoundConstructor
    public ShellRetentionStrategy(String command) {
        super(command);
    }

    @Extension
    public static final class DescriptorImpl extends ShellRetentionStrategyBase.Descriptor {
    }
}
