/******************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one     *
 * or more contributor license agreements.  See the NOTICE file   *
 * distributed with this work for additional information          *
 * regarding copyright ownership.  The ASF licenses this file     *
 * to you under the Apache License, Version 2.0 (the              *
 * "License"); you may not use this file except in compliance     *
 * with the License.  You may obtain a copy of the License at     *
 *                                                                *
 * http://www.apache.org/licenses/LICENSE-2.0                     *
 *                                                                *
 * Unless required by applicable law or agreed to in writing,     *
 * software distributed under the License is distributed on an    *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY         *
 * KIND, either express or implied.  See the License for the      *
 * specific language governing permissions and limitations        *
 * under the License.                                             *
 ******************************************************************/

package org.apache.james.cli;

import org.apache.james.cli.domain.DomainManage;
import picocli.CommandLine;

@CommandLine.Command(
        name = "james-cli",
        description = "James Webadmin CLI",
        mixinStandardHelpOptions = true,
        version = "1.0",
        subcommands = {
                DomainManage.class,
                CommandLine.HelpCommand.class
        }
)
public class WebAdminCli implements Runnable {

    public @CommandLine.Option(
            names = "--url",
            description = "James server URL",
            defaultValue = "127.0.0.1" //hard code for now easily develop on local server
    )
    String jamesUrl;

    public @CommandLine.Option(
            names = "--port",
            description = "James server Port number",
            defaultValue = "8000"
    )
    String jamesPort;

    @Override
    public void run() {

    }

    public static void main(String[] args) {
        new CommandLine(new WebAdminCli()).execute(args);
    }

}
