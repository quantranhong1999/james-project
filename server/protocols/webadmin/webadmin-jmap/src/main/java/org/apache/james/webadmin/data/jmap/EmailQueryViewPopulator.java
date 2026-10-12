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

package org.apache.james.webadmin.data.jmap;

import static jakarta.mail.Flags.Flag.DELETED;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.inject.Inject;

import org.apache.james.core.Username;
import org.apache.james.jmap.api.projections.EmailQueryView;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.model.FetchGroup;
import org.apache.james.mailbox.model.Mailbox;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.MessageResult;
import org.apache.james.mailbox.model.ThreadId;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.task.Task;
import org.apache.james.task.Task.Result;
import org.apache.james.util.ReactorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class EmailQueryViewPopulator {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailQueryViewPopulator.class);
    private static final Duration PERIOD = Duration.ofSeconds(1);
    private static final Username EMAIL_QUERY_VIEW_POPULATOR = Username.of("email-query-view-populator");

    static class Progress {
        private final AtomicLong processedMailboxCount;
        private final AtomicLong processedMessageCount;
        private final AtomicLong failedMailboxCount;
        private final AtomicLong failedMessageCount;

        Progress() {
            failedMailboxCount = new AtomicLong();
            processedMessageCount = new AtomicLong();
            processedMailboxCount = new AtomicLong();
            failedMessageCount = new AtomicLong();
        }

        private void incrementProcessedMailboxCount() {
            processedMailboxCount.incrementAndGet();
        }

        private void incrementProcessedMessageCount() {
            processedMessageCount.incrementAndGet();
        }

        private void incrementFailedMailboxCount() {
            failedMailboxCount.incrementAndGet();
        }

        private void incrementFailedMessageCount() {
            failedMessageCount.incrementAndGet();
        }

        long getProcessedMailboxCount() {
            return processedMailboxCount.get();
        }

        long getProcessedMessageCount() {
            return processedMessageCount.get();
        }

        long getFailedMailboxCount() {
            return failedMailboxCount.get();
        }

        long getFailedMessageCount() {
            return failedMessageCount.get();
        }
    }

    private final MailboxSessionMapperFactory mailboxSessionMapperFactory;
    private final MailboxManager mailboxManager;
    private final EmailQueryView emailQueryView;

    @Inject
    EmailQueryViewPopulator(MailboxSessionMapperFactory mailboxSessionMapperFactory,
                            MailboxManager mailboxManager,
                            EmailQueryView emailQueryView) {
        this.mailboxSessionMapperFactory = mailboxSessionMapperFactory;
        this.mailboxManager = mailboxManager;
        this.emailQueryView = emailQueryView;
    }

    Mono<Result> populateView(Progress progress, RunningOptions runningOptions) {
        return correctProjection(listAllMailboxMessages(progress), runningOptions, progress)
            .map(result -> progress.getFailedMailboxCount() > 0 ? Result.PARTIAL : result);
    }

    private Flux<MessageResult> listAllMailboxMessages(Progress progress) {
        // Walks every mailbox, whoever owns it: listing users would skip the mailboxes of owners that are not users.
        // Mailboxes are streamed from the mapper: MailboxManager::list would hold every mailbox path in memory for the whole run.
        MailboxSession session = mailboxManager.createSystemSession(EMAIL_QUERY_VIEW_POPULATOR);
        return mailboxSessionMapperFactory.getMailboxMapper(session)
            .list()
            .concatMap(mailbox -> listMailboxMessages(progress, mailbox))
            .filter(messageResult -> !messageResult.getFlags().contains(DELETED));
    }

    private Flux<MessageResult> listMailboxMessages(Progress progress, Mailbox mailbox) {
        MailboxSession ownerSession = mailboxManager.createSystemSession(mailbox.getUser());
        return Mono.from(mailboxManager.getMailboxReactive(mailbox.getMailboxId(), ownerSession))
            .flatMapMany(messageManager -> emailQueryView.delete(messageManager.getId())
                .thenMany(listAllMessages(messageManager, ownerSession)))
            .doOnComplete(progress::incrementProcessedMailboxCount)
            .onErrorResume(MailboxNotFoundException.class, e -> {
                LOGGER.info("JMAP emailQuery view re-computation skipped {} as it was deleted", mailbox.generateAssociatedPath().asString());
                return Flux.empty();
            })
            .onErrorResume(MailboxException.class, e -> {
                LOGGER.error("JMAP emailQuery view re-computation aborted for {} as we failed listing its messages", mailbox.generateAssociatedPath().asString(), e);
                progress.incrementFailedMailboxCount();
                return Flux.empty();
            });
    }

    private Mono<Result> correctProjection(MessageResult messageResult, Progress progress) {
        return Mono.fromCallable(() -> {
            MailboxId mailboxId = messageResult.getMailboxId();
            MessageId messageId = messageResult.getMessageId();
            ThreadId threadId = messageResult.getThreadId();
            ZonedDateTime receivedAt = ZonedDateTime.ofInstant(messageResult.getInternalDate().toInstant(), ZoneOffset.UTC);
            return new EmailQueryView.Entry(mailboxId, messageId, receivedAt, threadId);
        })
            .flatMap(entry -> emailQueryView.save(entry.getMailboxId(), entry.getReceivedAt(), entry.getMessageId(), entry.getThreadId()))
            .thenReturn(Result.COMPLETED)
            .doOnSuccess(any -> progress.incrementProcessedMessageCount())
            .onErrorResume(e -> {
                LOGGER.error("JMAP emailQuery view re-computation aborted for {} - {} - {}",
                    messageResult.getMailboxId(),
                    messageResult.getMessageId(),
                    messageResult.getUid(), e);
                progress.incrementFailedMessageCount();
                return Mono.just(Result.PARTIAL);
            });
    }

    private Mono<Result> correctProjection(Flux<MessageResult> entries, RunningOptions runningOptions, Progress progress) {
        return entries.transform(ReactorUtils.<MessageResult, Result>throttle()
                .elements(runningOptions.getMessagesPerSecond())
                .per(PERIOD)
                .forOperation(entry -> correctProjection(entry, progress)))
            .reduce(Task::combine)
            .switchIfEmpty(Mono.just(Result.COMPLETED));
    }

    private Flux<MessageResult> listAllMessages(MessageManager messageManager, MailboxSession session) {
        return Flux.from(messageManager.getMessagesReactive(MessageRange.all(), FetchGroup.HEADERS, session));
    }
}
