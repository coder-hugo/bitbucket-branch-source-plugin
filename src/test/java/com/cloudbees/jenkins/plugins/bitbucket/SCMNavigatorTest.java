/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.jenkins.plugins.bitbucket;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.cloudbees.jenkins.plugins.bitbucket.client.BitbucketCloudApiClient;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import hudson.model.TaskListener;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceObserver;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.SCMSourceObserver.ProjectObserver;

public class SCMNavigatorTest {

    @Test
    public void teamRepositoriesDiscovering() throws IOException, InterruptedException {
        BitbucketSCMNavigator navigator = new BitbucketSCMNavigator("myteam", null, null);
        navigator.setPattern("repo(.*)");
        navigator.setBitbucketConnector(getConnectorMock(RepositoryType.GIT, true));
        SCMSourceObserverImpl observer = new SCMSourceObserverImpl(BitbucketClientMockUtils.getTaskListenerMock());
        navigator.visitSources(observer);

        assertEquals("myteam", navigator.getRepoOwner());
        assertEquals("repo(.*)", navigator.getPattern());

        List<String> observed = observer.getObserved();
        // Only 2 repositores match the pattern
        assertTrue("There must be 2 repositories in the team, but was " + observed.size(), observed.size() == 2);
        assertEquals("repo1", observed.get(0));
        assertEquals("repo2", observed.get(1));

        List<ProjectObserver> observers = observer.getProjectObservers();
        for (ProjectObserver obs : observers) {
            List<SCMSource> sources = ((SCMSourceObserverImpl.ProjectObserverImpl) obs).getSources();
            // It should contain only one source
            assertTrue("Only one source must be created per observed repository", sources.size() == 1);
            SCMSource scmSource = sources.get(0);
            assertTrue("BitbucketSCMSource instances must be added", scmSource instanceof BitbucketSCMSource);
            // Check correct repoOwner (team name in this case) was set
            assertEquals(((BitbucketSCMSource) scmSource).getRepoOwner(), "myteam");
        }
    }

    private class SCMSourceObserverImpl extends SCMSourceObserver {

        List<String> observed = new ArrayList<String>();
        List<ProjectObserver> projectObservers = new ArrayList<SCMSourceObserver.ProjectObserver>();
        TaskListener listener;

        public SCMSourceObserverImpl(TaskListener listener) {
            this.listener = listener;
        }

        @Override
        public SCMSourceOwner getContext() {
            return null;
        }

        @Override
        public TaskListener getListener() {
            return listener;
        }

        @Override
        public ProjectObserver observe(String projectName) throws IllegalArgumentException {
            observed.add(projectName);
            ProjectObserverImpl obs = new ProjectObserverImpl();
            projectObservers.add(obs);
            return obs;
        }

        @Override
        public void addAttribute(String key, Object value) throws IllegalArgumentException, ClassCastException {
        }

        public List<String> getObserved() {
            return observed;
        }

        public List<ProjectObserver> getProjectObservers() {
            return projectObservers;
        }

        public class ProjectObserverImpl extends ProjectObserver {

            private List<SCMSource> sources = new ArrayList<SCMSource>();

            @Override
            public void addSource(SCMSource source) {
                sources.add(source);
            }

            @Override
            public void addAttribute(String key, Object value) throws IllegalArgumentException, ClassCastException {
            }

            @Override
            public void complete() throws IllegalStateException, InterruptedException {
            }

            public List<SCMSource> getSources() {
                return sources;
            }
        }
    }

    public static BitbucketApiConnector getConnectorMock(RepositoryType type, boolean includePullRequests) {
        BitbucketApiConnector mockConnector = mock(BitbucketApiConnector.class);
        BitbucketCloudApiClient mockedApi = BitbucketClientMockUtils.getAPIClientMock(type, includePullRequests);
        when(mockConnector.create(anyString(), any(StandardUsernamePasswordCredentials.class))).thenReturn(mockedApi);
        when(mockConnector.create(anyString(), anyString(), any(StandardUsernamePasswordCredentials.class))).thenReturn(mockedApi);
        return mockConnector;
    }

}