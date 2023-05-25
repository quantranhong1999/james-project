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

package org.apache.james.jmap.rfc8621.memory;

import static org.apache.james.data.UsersRepositoryModuleChooser.Implementation.DEFAULT;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.james.JamesServerBuilder;
import org.apache.james.JamesServerExtension;
import org.apache.james.MemoryJamesConfiguration;
import org.apache.james.MemoryJamesServerMain;
import org.apache.james.jmap.rfc8621.contract.EmailSubmissionSetMethodFutureReleaseContract;
import org.apache.james.mailbox.inmemory.InMemoryMessageId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.modules.TestJMAPServerModule;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.queue.api.MailQueueFactory;
import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.james.queue.api.RawMailQueueItemDecoratorFactory;
import org.apache.james.queue.memory.MemoryMailQueueFactory;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.inject.name.Names;

class MemoryEmailSubmissionSetMethodFutureReleaseTest implements EmailSubmissionSetMethodFutureReleaseContract {
    private static final Instant DATE = Instant.parse("2023-04-14T10:00:00.00Z");
    private static final Clock CLOCK = Clock.fixed(DATE, ZoneId.of("Z"));
    private static final MemoryMailQueueFactory queueFactory = new MemoryMailQueueFactory(new RawMailQueueItemDecoratorFactory(), CLOCK);
    private static final MemoryMailQueueFactory.MemoryCacheableMailQueue queue = queueFactory.createQueue(MailQueueFactory.SPOOL);
    @RegisterExtension
    static JamesServerExtension testExtension = new JamesServerBuilder<MemoryJamesConfiguration>(tmpDir ->
        MemoryJamesConfiguration.builder()
            .workingDirectory(tmpDir)
            .configurationFromClasspath()
            .usersRepository(DEFAULT)
            .build())
        .server(configuration -> MemoryJamesServerMain.createServer(configuration)
            .overrideWith(new TestJMAPServerModule()))
        .overrideServerModule(binder -> binder.bind(Clock.class).toInstance(CLOCK))
        .overrideServerModule(binder -> binder.bind(Boolean.class).annotatedWith(Names.named("supportsDelaySends")).toInstance(true))
        .overrideServerModule(binder -> binder.bind(MemoryMailQueueFactory.class).toInstance(queueFactory))
        .build();

    @Override
    public MessageId randomMessageId() {
        return InMemoryMessageId.of(ThreadLocalRandom.current().nextInt(100000) + 100);
    }

    @Override
    public ManageableMailQueue.MailQueueIterator mailQueueIterator() {
        return queue.browse();
    }

    @Override
    public MailQueue mailQueue() {
        return queue;
    }

}
