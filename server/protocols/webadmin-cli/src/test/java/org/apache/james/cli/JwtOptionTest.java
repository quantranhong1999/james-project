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

import static org.apache.james.MemoryJamesServerMain.IN_MEMORY_SERVER_AGGREGATE_MODULE;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;

import org.apache.james.GuiceJamesServer;
import org.apache.james.JamesServerBuilder;
import org.apache.james.JamesServerExtension;
import org.apache.james.jwt.JwtConfiguration;
import org.apache.james.jwt.JwtTokenVerifier;
import org.apache.james.util.ClassLoaderUtils;
import org.apache.james.util.Port;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.WebAdminGuiceProbe;
import org.apache.james.webadmin.authentication.AuthenticationFilter;
import org.apache.james.webadmin.authentication.JwtFilter;
import org.apache.james.webadmin.integration.WebadminIntegrationTestModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.inject.name.Names;

public class JwtOptionTest {

    @RegisterExtension
    static JamesServerExtension jamesServerExtension = new JamesServerBuilder<>(JamesServerBuilder.defaultConfigurationProvider())
        .server(configuration -> GuiceJamesServer.forConfiguration(configuration)
            .combineWith(IN_MEMORY_SERVER_AGGREGATE_MODULE)
            .overrideWith(new WebadminIntegrationTestModule())
            .overrideWith(binder -> binder.bind(AuthenticationFilter.class).to(JwtFilter.class))
            .overrideWith(binder -> binder.bind(JwtTokenVerifier.Factory.class)
                .annotatedWith(Names.named("webadmin"))
                .toInstance(() -> JwtTokenVerifier.create(jwtConfiguration()))))
        .build();

    protected static JwtConfiguration jwtConfiguration() {
        return new JwtConfiguration(
            Optional.of(ClassLoaderUtils.getSystemResourceAsString("jwt_publickey")));
    }

    private static final String VALID_TOKEN_ADMIN_TRUE = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbkBvcGVuL" +
        "XBhYXMub3JnIiwiYWRtaW4iOnRydWUsImlhdCI6MTQ4OTAzODQzOH0.rgxCkdWEa-92a4R-72a9Z49k4LRvQDShgci5Y7qWRUP9IGJCK-lMkrHF" +
        "4H0a6L87BYppxVW701zaZ6dNxRMvHnjLBBWnPsC2B0rkkr2hEL2zfz7sb-iNGV-J4ICx97t8-TfQ5rz3VOX0FwdusPL_rJtmlGEGRivPkR6_aBe1" +
        "kQnvMlwpqF_3ox58EUqYJk6lK_6rjKEV3Xfre31IMpuQUy6c7TKc95sL2-13cknelTierBEmZ00RzTtv9SHIEfzZTfaUK2Wm0PvnQjmU2nIdEvU" +
        "EqE-jrM3yYXcQzoO-YTQnEhdl-iqbCfmEpYkl2Bx3eIq7gRxxnr7BPsX6HrCB0w";

    private static final String VALID_TOKEN_ADMIN_FALSE = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbkBvcGVu" +
        "LXBhYXMub3JnIiwiYWRtaW4iOmZhbHNlLCJpYXQiOjE0ODkwNDA4Njd9.reQc3DiVvbQHF08oW1qOUyDJyv3tfzDNk8jhVZequiCdOI9vXnRlOe" +
        "-yDYktd4WT8MYhqY7MgS-wR0vO9jZFv8ZCgd_MkKCvCO0HmMjP5iQPZ0kqGkgWUH7X123tfR38MfbCVAdPDba-K3MfkogV1xvDhlkPScFr_6MxE" +
        "xtedOK2JnQZn7t9sUzSrcyjWverm7gZkPptkIVoS8TsEeMMME5vFXe_nqkEG69q3kuBUm_33tbR5oNS0ZGZKlG9r41lHBjyf9J1xN4UYV8n866d" +
        "a7RPPCzshIWUtO0q9T2umWTnp-6OnOdBCkndrZmRR6pPxsD5YL0_77Wq8KT_5__fGA";

    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorStreamCaptor = new ByteArrayOutputStream();

    private DataProbeImpl dataProbe;

    @Test
    void jwtAuthenticationWithValidTokenShouldSucceed(GuiceJamesServer server) throws Exception {
        Port port = server.getProbe(WebAdminGuiceProbe.class).getWebAdminPort();
        dataProbe = server.getProbe(DataProbeImpl.class);

        int exitCode = WebAdminCli.executeFluent(new PrintStream(outputStreamCaptor), new PrintStream(errorStreamCaptor),
            "--url", "http://127.0.0.1:" + port.getValue(), "--jwt", VALID_TOKEN_ADMIN_TRUE, "domain", "create", "linagora.com");

        assertThat(exitCode).isEqualTo(0);
        assertThat(dataProbe.listDomains()).contains("linagora.com");
    }

    @Test
    void jwtAuthenticationWithInvalidTokenShouldRejectRequests(GuiceJamesServer server) throws Exception {
        Port port = server.getProbe(WebAdminGuiceProbe.class).getWebAdminPort();
        dataProbe = server.getProbe(DataProbeImpl.class);

        int exitCode = WebAdminCli.executeFluent(new PrintStream(outputStreamCaptor), new PrintStream(errorStreamCaptor),
            "--url", "http://127.0.0.1:" + port.getValue(), "--jwt", VALID_TOKEN_ADMIN_FALSE, "domain", "create", "linagora.com");

        assertThat(exitCode).isEqualTo(1);
        assertThat(dataProbe.listDomains()).containsOnly("localhost");
    }

    @Test
    void jwtAuthenticationWithNonTokenShouldRejectRequests(GuiceJamesServer server) throws Exception {
        Port port = server.getProbe(WebAdminGuiceProbe.class).getWebAdminPort();
        dataProbe = server.getProbe(DataProbeImpl.class);

        int exitCode = WebAdminCli.executeFluent(new PrintStream(outputStreamCaptor), new PrintStream(errorStreamCaptor),
            "--url", "http://127.0.0.1:" + port.getValue(), "domain", "create", "linagora.com");

        assertThat(exitCode).isEqualTo(1);
        assertThat(dataProbe.listDomains()).containsOnly("localhost");
    }

}
