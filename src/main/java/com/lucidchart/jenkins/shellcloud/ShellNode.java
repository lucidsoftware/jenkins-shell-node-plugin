package com.lucidchart.jenkins.shellcloud;

import hudson.Extension;

public class ShellNode {
    private ShellNode() {
    }

    //@Extension
    public static class ShellNodeDescriptor extends ShellNodeBase.Descriptor {
    }
}
