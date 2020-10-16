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
        name = "list",
        description = "Show all domains on the domains list"
)
public class DomainListCommand implements Runnable {

    @CommandLine.ParentCommand
    DomainManage domainManage;

    @Override
    public void run() {
        try {
            DomainClient domainClient = Feign.builder()
                    .decoder(new GsonDecoder())
                    .target(DomainClient.class,"http://" + domainManage.webAdminCli.jamesUrl + ":" + domainManage.webAdminCli.jamesPort + "/domains");
            if (domainClient.getDomainList().size() == 0) System.out.println("There is no domain available on the domains list.");
                else domainClient.getDomainList().forEach(domainName -> System.out.println(domainName));
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
