/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.scripting.graphql.it;

import javax.inject.Inject;
import javax.script.ScriptEngineFactory;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.apache.sling.testing.paxexam.TestSupport;
import org.junit.Before;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.options.ModifiableCompositeOption;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.service.cm.ConfigurationAdmin;

import org.apache.sling.engine.SlingRequestProcessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;
import static org.apache.sling.testing.paxexam.SlingOptions.slingResourcePresence;
import static org.apache.sling.testing.paxexam.SlingOptions.slingScripting;
import static org.apache.sling.testing.paxexam.SlingOptions.slingScriptingJsp;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

import java.util.ArrayList;
import java.util.List;

public abstract class GraphQLScriptingTestSupport extends TestSupport {

    private final static int STARTUP_WAIT_SECONDS = 30;

    @Inject
    @Filter(value = "(names=graphql)")
    protected ScriptEngineFactory scriptEngineFactory;

    @Inject
    private ResourceResolverFactory resourceResolverFactory;

    @Inject
    protected SlingRequestProcessor requestProcessor;

    public ModifiableCompositeOption baseConfiguration() {
        final String vmOpt = System.getProperty("pax.vm.options");

        return composite(
            when(vmOpt != null).useOptions(
                vmOption(vmOpt)
            ),
            super.baseConfiguration(),
            slingQuickstart(),
            graphQLJava(),
            testBundle("bundle.filename"),
            newConfiguration("org.apache.sling.jcr.base.internal.LoginAdminWhitelist")
                .put("whitelist.bundles.regexp", "^PAXEXAM.*$")
                .asOption(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.servlet-helpers").versionAsInProject(),
            mavenBundle().groupId("com.google.code.gson").artifactId("gson").versionAsInProject(),
            slingResourcePresence(),
            junitBundles()
        );
    }

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(final TestProbeBuilder testProbeBuilder) {
        testProbeBuilder.setHeader("Sling-Initial-Content", "initial-content");
        return testProbeBuilder;
    }

    protected Option slingQuickstart() {
        final int httpPort = findFreePort();
        final String workingDirectory = workingDirectory();
        return composite(
            slingQuickstartOakTar(workingDirectory, httpPort),
            slingScripting(),
            slingScriptingJsp()
        );
    }

    protected Option graphQLJava() {
        return composite(
            mavenBundle().groupId("com.graphql-java").artifactId("graphql-java").versionAsInProject(),
            mavenBundle().groupId("org.antlr").artifactId("antlr4-runtime").versionAsInProject(),
            mavenBundle().groupId("com.graphql-java").artifactId("java-dataloader").versionAsInProject(),
            mavenBundle().groupId("org.reactivestreams").artifactId("reactive-streams").versionAsInProject()
        );
    }

    /**
     * Injecting the appropriate services to wait for would be more elegant but this is very reliable..
     */
    @Before
    public void waitForSling() throws Exception {
        final int expectedStatus = 200;
        final List<Integer> statuses = new ArrayList<>();
        final String path = "/.json";
        final long endTime = System.currentTimeMillis() + STARTUP_WAIT_SECONDS * 1000;

        while (System.currentTimeMillis() < endTime) {
            final int status = executeRequest("GET", path, -1).getStatus();
            statuses.add(status);
            if (status == expectedStatus) {
                return;
            }
            Thread.sleep(250);
        }

        fail("Did not get a " + expectedStatus + " status at " + path + " got " + statuses);
    }

    protected MockSlingHttpServletResponse executeRequest(final String method, final String path, final int expectedStatus) throws Exception {
        final ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
        assertNotNull("Expecting ResourceResolver", resourceResolver);
        final MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(resourceResolver) {
            @Override
            public String getMethod() {
                return method;
            }
        };

        request.setPathInfo(path);
        final MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();
        requestProcessor.processRequest(request, response, resourceResolver);

        if (expectedStatus > 0) {
            assertEquals("Expected status " + expectedStatus + " for " + method
                + " at " + path + " - content=" + response.getOutputAsString(), expectedStatus, response.getStatus());
        }

        return response;
    }

    protected String getContent(String path) throws Exception {
        return executeRequest("GET", path, 200).getOutputAsString();
    }
}