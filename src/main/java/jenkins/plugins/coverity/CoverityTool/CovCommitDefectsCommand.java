/*******************************************************************************
 * Copyright (c) 2018 Synopsys, Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Synopsys, Inc - initial implementation and documentation
 *******************************************************************************/
package jenkins.plugins.coverity.CoverityTool;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import jenkins.plugins.coverity.*;
import jenkins.plugins.coverity.ws.CimServiceUrlCache;
import org.apache.commons.lang.StringUtils;

import java.net.URL;

public class CovCommitDefectsCommand extends CoverityCommand {

    private static final String command = "cov-commit-defects";
    private static final String httpsPort = "--https-port";
    private static final String streamArg = "--stream";
    private static final String userArg = "--user";
    private static final String coverity_passphrase = "COVERITY_PASSPHRASE";

    private CIMInstance cimInstance;
    private CIMStream cimStream;
    private InvocationAssistance invocationAssistance;

    public CovCommitDefectsCommand(
            AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener, CoverityPublisher publisher, String home, EnvVars envVars,
            CIMStream cimStream, CIMInstance cimInstance) {
        super(command, build, launcher, listener, publisher, home, envVars);
        this.cimStream = cimStream;
        this.cimInstance = cimInstance;

        if (publisher != null && publisher.getInvocationAssistance() != null) {
            invocationAssistance = publisher.getInvocationAssistance();
        }
    }

    @Override
    protected void prepareCommand() {
        addServerInfo();

        addStream();
        addUserInfo();
        addCommitArguments();
        listener.getLogger().println("[Coverity] cov-commit-defects command line arguments: " + commandLine.toString());
    }

    @Override
    protected boolean canExecute() {
        if (publisher.getInvocationAssistance() == null) {
            return false;
        }
        return true;
    }

    private void addServerInfo() {
        URL url = CimServiceUrlCache.getInstance().getURL(cimInstance);
        addHost(url, cimInstance);

        boolean isSslConfigured = isSslConfigured(url, cimInstance);
        if (isSslConfigured){
            addArgument(useSslArg);
            addHttpsPort(url);
            addSslConfiguration();
        } else{
            addPort(url, cimInstance);
        }
    }

    private void addHttpsPort(URL url) {
        addArgument(httpsPort);

        if (url == null){
            addArgument(Integer.toString(cimInstance.getPort()));
        }else{
            String portNumber = Integer.toString(url.getPort());
            if (portNumber.equals("-1")){
                addArgument("443");
            }else{
                addArgument(Integer.toString(url.getPort()));
            }
        }
    }

    private void addStream() {
        addArgument(streamArg);
        addArgument(CoverityUtils.doubleQuote(cimStream.getStream(), invocationAssistance.getUseAdvancedParser()));
    }

    private void addUserInfo() {
        addArgument(userArg);
        addArgument(cimInstance.getCoverityUser());
        envVars.put(coverity_passphrase, cimInstance.getCoverityPassword());
    }

    private void addCommitArguments() {
        if (!StringUtils.isEmpty(invocationAssistance.getCommitArguments())) {
            try{
                addArguments(EnvParser.tokenize(invocationAssistance.getCommitArguments()));
            }catch(ParseException e) {
                throw new RuntimeException("ParseException occurred during tokenizing the cov-commit-defect commit arguments.");
            }
        }
    }
}
