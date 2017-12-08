package com.lucidchart.jenkins.commandnode;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

public class CommandRetentionStrategy extends CommandRetentionStrategyBase {
    @DataBoundConstructor
    public CommandRetentionStrategy(String command) {
        super(command);
    }

    @Extension
    public static final class DescriptorImpl extends CommandRetentionStrategyBase.Descriptor {
    }
}
