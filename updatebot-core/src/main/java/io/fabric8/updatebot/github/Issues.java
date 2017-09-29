/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.updatebot.github;

import io.fabric8.updatebot.Configuration;
import io.fabric8.updatebot.commands.CommandContext;
import io.fabric8.updatebot.kind.Kind;
import io.fabric8.updatebot.model.DependencyVersionChange;
import io.fabric8.updatebot.support.Markdown;
import io.fabric8.updatebot.support.Strings;
import io.fabric8.utils.Objects;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.fabric8.updatebot.github.GitHubHelpers.retryGithub;

/**
 */
public class Issues {
    public static final String BODY = Markdown.UPDATEBOT_ICON + " cannot update some dependency versions until other projects are released to fix dependency conflicts.\n\n" +
            "This issue is used to coordinate version changes on this repository coming from other repositories and will be closed once all the version conflicts are resolved.";
    public static final String CLOSE_MESSAGE = Markdown.UPDATEBOT_ICON + " closing as no more dependency conflicts while ";
    public static final String PENDING_CHANGE_COMMENT_PREFIX = Markdown.UPDATEBOT_ICON + " detected conflicts while ";
    private static final transient Logger LOG = LoggerFactory.getLogger(Issues.class);

    public static List<GHIssue> getOpenIssues(GHRepository ghRepository, Configuration configuration) throws IOException {
        String label = configuration.getGithubPullRequestLabel();
        return getOpenIssues(ghRepository, label);
    }

    public static List<GHIssue> getOpenIssues(GHRepository ghRepository, String label) throws IOException {
        List<GHIssue> issues = retryGithub(() -> ghRepository.getIssues(GHIssueState.OPEN));
        List<GHIssue> answer = new ArrayList<>();
        for (GHIssue issue : issues) {
            if (GitHubHelpers.hasLabel(getLabels(issue), label) && !issue.isPullRequest()) {
                answer.add(issue);
            }
        }
        return answer;
    }


    public static List<DependencyVersionChange> loadPendingChangesFromIssue(CommandContext context, GHIssue issue) throws IOException {
        List<GHIssueComment> comments = issue.getComments();
        String lastCommand = null;
        for (GHIssueComment comment : comments) {
            String command = updateBotIssuePendingChangesComment(context, comment);
            if (command != null) {
                lastCommand = command;
            }
        }
        if (lastCommand == null) {
            LOG.warn("No UpdateBot comment found on issue " + issue.getHtmlUrl());
            return new ArrayList<>();
        }
        return parseUpdateBotIssuePendingChangesComment(lastCommand);
    }

    public static List<DependencyVersionChange> parseUpdateBotIssuePendingChangesComment(String command) {
        String[] lines = command.split("\n");
        List<DependencyVersionChange> answer = new ArrayList<>();
        for (String line : lines) {
            String text = line.trim();
            if (Strings.notEmpty(text)) {
                addChangeFromCommentLine(answer, text);
            }
        }
        return answer;
    }

    protected static void addChangeFromCommentLine(List<DependencyVersionChange> answer, String text) {
        String[] words = text.split("\\s+");
        if (words.length < 3) {
            LOG.warn("Ignoring command: Not enough arguments: " + text);
            return;
        }
        Kind kind = null;
        try {
            kind = Kind.fromName(words[0]);
        } catch (IllegalArgumentException e) {
            LOG.warn("Ignoring command: no such kind `" + words[0] + "` in: " + text);
            return;
        }
        if (kind != null) {
            DependencyVersionChange change;
            String dependency = words[1];
            String version = words[2];
            String scope = null;
            if (words.length > 3) {
                scope = words[3];
            }
            if (scope == null) {
                change = new DependencyVersionChange(kind, dependency, version);
            } else {
                change = new DependencyVersionChange(kind, dependency, version, scope);
            }
            answer.add(change);
        }
    }

    public static String createPendingChangesCommentCommand(List<DependencyVersionChange> changes) {
        StringBuilder builder = new StringBuilder();
        for (DependencyVersionChange change : changes) {
            builder.append("\n    ");
            String scope = change.getScope();
            if (scope == null) {
                scope = "";
            }
            builder.append(String.join(" ", change.getKind().toString(), change.getDependency(), change.getVersion(), scope));
        }
        return builder.toString();
    }


    public static void addPendingChangesComment(GHIssue issue, List<DependencyVersionChange> pendingChanges, String operationDescription) throws IOException {
        String comment = PENDING_CHANGE_COMMENT_PREFIX + operationDescription + "\n" + createPendingChangesCommentCommand(pendingChanges);
        issue.comment(comment);
    }


    public static String updateBotIssuePendingChangesComment(CommandContext context, GHIssueComment comment) throws IOException {
        GHUser user = comment.getUser();
        if (user != null) {
            if (Objects.equal(context.getConfiguration().getGithubUsername(), user.getLogin())) {
                String body = comment.getBody();
                if (body != null) {
                    body = body.trim();
                    if (body.startsWith(PENDING_CHANGE_COMMENT_PREFIX)) {
                        return body;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Lets try find the issue
     */
    public static GHIssue findIssue(CommandContext context, List<GHIssue> issues) {
        String prefix = context.createIssueTitlePrefix();
        if (issues != null) {
            for (GHIssue issue : issues) {
                String title = issue.getTitle();
                if (title != null && title.startsWith(prefix)) {
                    return issue;
                }
            }
        }
        return null;
    }

    public static GHIssue createIssue(CommandContext context, GHRepository repository) throws IOException {
        return repository.createIssue(context.createIssueTitlePrefix()).
                body(BODY).
                label(context.getConfiguration().getGithubPullRequestLabel()).
                create();
    }

    public static void logOpen(List<GHIssue> issues) {
        for (GHIssue issue : issues) {
            LOG.info("Open issue " + issue.getHtmlUrl());
        }
    }

    /**
     * Lets return the labels on an issue with retries
     */
    public static Collection<GHLabel> getLabels(GHIssue issue) throws IOException {
        return retryGithub(() -> issue.getLabels());
    }
}