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
package io.jenkins.updatebot.kind;

import io.jenkins.updatebot.kind.brew.BrewUpdater;
import io.jenkins.updatebot.kind.docker.DockerUpdater;
import io.jenkins.updatebot.kind.file.FileUpdater;
import io.jenkins.updatebot.kind.helm.HelmUpdater;
import io.jenkins.updatebot.kind.make.MakeUpdater;
import io.jenkins.updatebot.kind.maven.MavenUpdater;
import io.jenkins.updatebot.kind.npm.PackageJsonUpdater;
import io.jenkins.updatebot.kind.plugins.PluginsUpdater;
import io.jenkins.updatebot.kind.regex.RegexUpdater;

/**
 */
public enum Kind {
    FILE("file", new FileUpdater()),
    HELM("helm", new HelmUpdater()),
    MAVEN("maven", new MavenUpdater()),
    NPM("npm", new PackageJsonUpdater()),
    PLUGINS("plugins", new PluginsUpdater()),
    BREW("brew", new BrewUpdater()),
    DOCKER("docker", new DockerUpdater()),
    MAKE("make", new MakeUpdater()),
    REGEX("regex", new RegexUpdater());

    private String name;
    private Updater updater;

    Kind(String name, Updater updater) {
        this.name = name;
        this.updater = updater;
    }

    /**
     * Returns the kind for the given name or null if it could not be found
     */
    public static Kind fromName(String name) {
        for (Kind value : values()) {
            if (name.equalsIgnoreCase(value.getName())) {
                return value;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public Updater getUpdater() {
        return updater;
    }
}
