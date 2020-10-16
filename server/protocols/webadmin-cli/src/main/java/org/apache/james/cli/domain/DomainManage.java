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

package org.apache.james.cli.domain;

import feign.Feign;
import feign.gson.GsonDecoder;
import org.apache.james.cli.WebAdminCli;
import org.apache.james.httpclient.DomainClient;
import picocli.CommandLine;

@CommandLine.Command(
        name = "domain",
        description = "Manage Domains",
        subcommands = {
                DomainListCommand.class
        }
)
public class DomainManage implements Runnable {

    @CommandLine.ParentCommand WebAdminCli webAdminCli;

    @Override
    public void run() {
        System.out.println("Please choose what to do with domain entity\n" +
                "Use 'help domain' for more informations");
    }

}