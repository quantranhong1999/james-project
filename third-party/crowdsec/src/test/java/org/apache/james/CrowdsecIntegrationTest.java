/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james;

import static org.apache.james.data.UsersRepositoryModuleChooser.Implementation.DEFAULT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;

import java.io.EOFException;
import java.io.IOException;
import java.util.stream.IntStream;

import org.apache.james.modules.protocols.ImapGuiceProbe;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.TestIMAPClient;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CrowdsecIntegrationTest {
    @RegisterExtension
    static CrowdsecExtension crowdsecExtension = new CrowdsecExtension();

    @RegisterExtension
    static JamesServerExtension testExtension = new JamesServerBuilder<MemoryJamesConfiguration>(tmpDir ->
        MemoryJamesConfiguration.builder()
            .workingDirectory(tmpDir)
            .configurationFromClasspath()
            .usersRepository(DEFAULT)
            .build())
        .server(MemoryJamesServerMain::createServer)
        .extension(crowdsecExtension)
        .lifeCycle(JamesServerExtension.Lifecycle.PER_TEST)
        .build();

    @RegisterExtension
    public TestIMAPClient testIMAPClient = new TestIMAPClient();

    private static final String DOMAIN = "domain.tld";
    private static final String BOB = "bob@" + DOMAIN;
    private static final String BOB_PASSWORD = "bobPassword";
    private static final String BAD_PASSWORD = "badPassword";
    private static final ConditionFactory CALMLY_AWAIT = Awaitility
        .with().pollInterval(ONE_HUNDRED_MILLISECONDS)
        .and().pollDelay(ONE_HUNDRED_MILLISECONDS)
        .await();

    @BeforeEach
    void setup(GuiceJamesServer server) throws Exception {
        server.getProbe(DataProbeImpl.class).fluent()
            .addDomain(DOMAIN)
            .addUser(BOB, BOB_PASSWORD);
    }

    @Test
    void ipShouldBeBannedByCrowdSecWhenFailingToImapLoginThreeTimes(GuiceJamesServer server) {
        // GIVEN an IP failed to log in 3 consecutive times in a short period
        IntStream.range(0, 3)
            .forEach(any ->
                assertThatThrownBy(() -> testIMAPClient.connect("127.0.0.1", server.getProbe(ImapGuiceProbe.class).getImapPort())
                    .login(BOB, BAD_PASSWORD))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Login failed"));

        // THEN connection from the IP would be blocked. CrowdSec takes time to processing the ban decision therefore the await below.
        CALMLY_AWAIT.atMost(Durations.TEN_SECONDS)
            .untilAsserted(() -> assertThatThrownBy(() -> testIMAPClient.connect("127.0.0.1", server.getProbe(ImapGuiceProbe.class).getImapPort()))
                .isInstanceOf(EOFException.class)
                .hasMessage("Connection closed without indication."));
    }
}
