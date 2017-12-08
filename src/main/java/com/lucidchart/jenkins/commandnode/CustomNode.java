package com.lucidchart.jenkins.commandnode;

import hudson.Extension;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

@Extension
public class CustomNode extends CustomNodeBase {

    @RequirePOST
    public HttpResponse doCreateNode(@QueryParameter String config) {
        return super.doCreateNode(config);
    }

}
