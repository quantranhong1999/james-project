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

package org.apache.james.httpclient;

import feign.Feign;
import feign.gson.GsonDecoder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class DomainClientTest {

    private static final String JAMES_URL = "127.0.0.1";
    private static final String JAMES_PORT = "8000";

    @Test
    void getDomainListShouldWork() {
        DomainClient domainClient = Feign.builder()
                .decoder(new GsonDecoder())
                .target(DomainClient.class,"http://" + JAMES_URL + ":" + JAMES_PORT + "/domains");
        domainClient.getDomainList().forEach(domainName -> assertThat(domainName instanceof String).isTrue());
        assertThat(domainClient.getDomainList().contains("localhost")); // default domain
    }

}
