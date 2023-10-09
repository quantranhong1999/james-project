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

package org.apache.james.mailbox.cassandra.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.james.core.Username;
import org.apache.james.mailbox.exception.SubscriptionException;
import org.apache.james.mailbox.store.user.SubscriptionMapper;
import org.apache.james.mailbox.store.user.SubscriptionMapperTest;
import org.apache.james.mailbox.store.user.model.Subscription;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Testcontainers
public class PostgresSubscriptionMapperTest extends SubscriptionMapperTest {
    static final String DB_NAME = "james-db";
    static final String DB_USER = "james";
    static final String DB_PASSWORD = "secret";
    @Container
    private static final GenericContainer<?> container = new PostgreSQLContainer("postgres:11.1")
        .withDatabaseName(DB_NAME)
        .withUsername(DB_USER)
        .withPassword(DB_PASSWORD)
        .withInitScript("postgres-subscription-init.sql");

    static PostgresqlConnectionFactory postgresqlConnectionFactory;

    @BeforeAll
    static void beforeAll() {
        System.out.println("Postgres port: " + container.getMappedPort(5432));
        Map<String, String> options = new HashMap<>();
        options.put("lock_timeout", "10s");
        postgresqlConnectionFactory = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
            .host(container.getHost())
            .port(container.getMappedPort(5432))
            .username(DB_USER)
            .password(DB_PASSWORD)
            .database(DB_NAME)
            .options(options)
            .build());
    }

    @BeforeEach
    void beforeEach() {
        // clean data
        Flux.usingWhen(
                postgresqlConnectionFactory.create(),
                connection -> Mono
                    .from(connection.createStatement("DELETE FROM subscription ").execute())
                    .map(Result::getRowsUpdated),
                Connection::close)
            .blockFirst();
    }

    protected SubscriptionMapper createSubscriptionMapper() {
        return new PostgresSubscriptionMapper(postgresqlConnectionFactory);
    }

    @Test
    void rlsExperimentSetup() throws SubscriptionException {
        Subscription subscriptionA = new Subscription(Username.of("usera@a.com"), "mailbox1");
        testee.save(subscriptionA);
        Subscription subscriptionB = new Subscription(Username.of("userb@b.com"), "mailbox2");
        testee.save(subscriptionB);

        List<Subscription> results = testee.findSubscriptionsForUser(Username.of("usera@a.com"));

        assertThat(results).containsOnly(subscriptionA);
    }
}
