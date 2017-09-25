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
package io.fabric8.updatebot;

import com.beust.jcommander.JCommander;
import io.fabric8.updatebot.commands.Help;
import io.fabric8.updatebot.commands.PullVersionChanges;
import io.fabric8.updatebot.commands.PushVersionChanges;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 */
public class UpdateBot {
    private static final transient Logger LOG = LoggerFactory.getLogger(UpdateBot.class);

    public static void main(String[] args) {
        try {
            PushVersionChanges pushVersionChanges = new PushVersionChanges();
            PullVersionChanges pullVersionChanges = new PullVersionChanges();
            Help help = new Help();

            JCommander commander = JCommander.newBuilder()
                    .addCommand("push", pushVersionChanges)
                    .addCommand("pull", pullVersionChanges)
                    .addCommand("help", help)
                    .build();
            commander.setProgramName("updatebot");
            commander.parse(args);

            String parsedCommand = commander.getParsedCommand();
            if (parsedCommand == null) {
                commander.usage();
            } else {
                switch (parsedCommand) {
                    case "push":
                        pushVersionChanges.run();
                        return;

                    case "pull":
                        pullVersionChanges.run();
                        return;

                    case "help":
                        help.run(commander);
                        return;

                    default:
                        commander.usage();
                }
            }
        } catch (IOException e) {
            System.err.println("Failed: " + e);
            e.printStackTrace();
            Throwable cause = e.getCause();
            if (cause != e) {
                System.out.println("Caused by: " + cause);
                cause.printStackTrace();
            }
            System.exit(1);
        }
    }
}
