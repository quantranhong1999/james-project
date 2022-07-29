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

package org.apache.james.rspamd.task;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MessageIdManager;
import org.apache.james.mailbox.model.MessageResult;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.rspamd.client.RSpamDHttpClient;
import org.apache.james.task.Task;
import org.apache.james.task.TaskExecutionDetails;
import org.apache.james.task.TaskType;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.util.ReactorUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.fge.lambdas.Throwing;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;

import reactor.core.publisher.Mono;

public class FeedHamToRSpamDTask implements Task {
    public static final TaskType TASK_TYPE = TaskType.of("FeedHamToRSpamDTask");

    public static class RunningOptions {
        public static final Optional<Long> DEFAULT_PERIOD = Optional.empty();
        public static final int DEFAULT_MESSAGES_PER_SECOND = 10;
        public static final double DEFAULT_SAMPLING_PROBABILITY = 1;
        public static final RunningOptions DEFAULT = new RunningOptions(DEFAULT_PERIOD, DEFAULT_MESSAGES_PER_SECOND,
            DEFAULT_SAMPLING_PROBABILITY);

        private final Optional<Long> periodInSecond;
        private final int messagesPerSecond;
        private final double samplingProbability;

        public RunningOptions(@JsonProperty("periodInSecond") Optional<Long> periodInSecond,
                              @JsonProperty("messagesPerSecond") int messagesPerSecond,
                              @JsonProperty("samplingProbability") double samplingProbability) {
            this.periodInSecond = periodInSecond;
            this.messagesPerSecond = messagesPerSecond;
            this.samplingProbability = samplingProbability;
        }

        public Optional<Long> getPeriodInSecond() {
            return periodInSecond;
        }

        public int getMessagesPerSecond() {
            return messagesPerSecond;
        }

        public double getSamplingProbability() {
            return samplingProbability;
        }
    }

    public static class AdditionalInformation implements TaskExecutionDetails.AdditionalInformation {

        private static AdditionalInformation from(Context context) {
            Context.Snapshot snapshot = context.snapshot();
            return new AdditionalInformation(
                Clock.systemUTC().instant(),
                snapshot.getHamMessageCount(),
                snapshot.getReportedHamMessageCount(),
                snapshot.getErrorCount(),
                snapshot.getMessagesPerSecond(),
                snapshot.getPeriod(),
                snapshot.getSamplingProbability());
        }

        private final Instant timestamp;
        private final long hamMessageCount;
        private final long reportedHamMessageCount;
        private final long errorCount;
        private final int messagesPerSecond;
        private final Optional<Long> period;
        private final double samplingProbability;

        public AdditionalInformation(Instant timestamp, long hamMessageCount, long reportedHamMessageCount, long errorCount, int messagesPerSecond, Optional<Long> period, double samplingProbability) {
            this.timestamp = timestamp;
            this.hamMessageCount = hamMessageCount;
            this.reportedHamMessageCount = reportedHamMessageCount;
            this.errorCount = errorCount;
            this.messagesPerSecond = messagesPerSecond;
            this.period = period;
            this.samplingProbability = samplingProbability;
        }

        public long getHamMessageCount() {
            return hamMessageCount;
        }

        public long getReportedHamMessageCount() {
            return reportedHamMessageCount;
        }

        public long getErrorCount() {
            return errorCount;
        }

        public int getMessagesPerSecond() {
            return messagesPerSecond;
        }

        public Optional<Long> getPeriod() {
            return period;
        }

        public double getSamplingProbability() {
            return samplingProbability;
        }

        @Override
        public Instant timestamp() {
            return timestamp;
        }
    }

    public static class Context {

        public static class Snapshot {

            public static Builder builder() {
                return new Builder();
            }

            static class Builder {
                private Optional<Long> hamMessageCount;
                private Optional<Long> reportedHamMessageCount;
                private Optional<Long> errorCount;
                private Optional<Integer> messagesPerSecond;
                private Optional<Long> period;
                private Optional<Double> samplingProbability;

                Builder() {
                    hamMessageCount = Optional.empty();
                    reportedHamMessageCount = Optional.empty();
                    errorCount = Optional.empty();
                    messagesPerSecond = Optional.empty();
                    period = Optional.empty();
                    samplingProbability = Optional.empty();
                }

                public Snapshot build() {
                    return new Snapshot(
                        hamMessageCount.orElse(0L),
                        reportedHamMessageCount.orElse(0L),
                        errorCount.orElse(0L),
                        messagesPerSecond.orElse(0),
                        period,
                        samplingProbability.orElse(1D));
                }

                public Builder hamMessageCount(long hamMessageCount) {
                    this.hamMessageCount = Optional.of(hamMessageCount);
                    return this;
                }

                public Builder reportedHamMessageCount(long reportedHamMessageCount) {
                    this.reportedHamMessageCount = Optional.of(reportedHamMessageCount);
                    return this;
                }

                public Builder errorCount(long errorCount) {
                    this.errorCount = Optional.of(errorCount);
                    return this;
                }

                public Builder messagesPerSecond(int messagesPerSecond) {
                    this.messagesPerSecond = Optional.of(messagesPerSecond);
                    return this;
                }

                public Builder period(Optional<Long> period) {
                    this.period = period;
                    return this;
                }

                public Builder samplingProbability(double samplingProbability) {
                    this.samplingProbability = Optional.of(samplingProbability);
                    return this;
                }
            }

            private final long hamMessageCount;
            private final long reportedHamMessageCount;
            private final long errorCount;
            private final int messagesPerSecond;
            private final Optional<Long> period;
            private final double samplingProbability;

            public Snapshot(long hamMessageCount, long reportedHamMessageCount, long errorCount, int messagesPerSecond, Optional<Long> period,
                            double samplingProbability) {
                this.hamMessageCount = hamMessageCount;
                this.reportedHamMessageCount = reportedHamMessageCount;
                this.errorCount = errorCount;
                this.messagesPerSecond = messagesPerSecond;
                this.period = period;
                this.samplingProbability = samplingProbability;
            }

            public long getHamMessageCount() {
                return hamMessageCount;
            }

            public long getReportedHamMessageCount() {
                return reportedHamMessageCount;
            }

            public long getErrorCount() {
                return errorCount;
            }

            public int getMessagesPerSecond() {
                return messagesPerSecond;
            }

            public Optional<Long> getPeriod() {
                return period;
            }

            public double getSamplingProbability() {
                return samplingProbability;
            }

            @Override
            public final boolean equals(Object o) {
                if (o instanceof Snapshot) {
                    Snapshot snapshot = (Snapshot) o;

                    return Objects.equals(this.hamMessageCount, snapshot.hamMessageCount)
                        && Objects.equals(this.reportedHamMessageCount, snapshot.reportedHamMessageCount)
                        && Objects.equals(this.errorCount, snapshot.errorCount)
                        && Objects.equals(this.messagesPerSecond, snapshot.messagesPerSecond)
                        && Objects.equals(this.samplingProbability, snapshot.samplingProbability)
                        && Objects.equals(this.period, snapshot.period);
                }
                return false;
            }

            @Override
            public final int hashCode() {
                return Objects.hash(hamMessageCount, reportedHamMessageCount, errorCount, messagesPerSecond, period, samplingProbability);
            }

            @Override
            public String toString() {
                return MoreObjects.toStringHelper(this)
                    .add("hamMessageCount", hamMessageCount)
                    .add("reportedHamMessageCount", reportedHamMessageCount)
                    .add("errorCount", errorCount)
                    .add("messagesPerSecond", messagesPerSecond)
                    .add("period", period)
                    .add("samplingProbability", samplingProbability)
                    .toString();
            }
        }

        private final AtomicLong hamMessageCount;
        private final AtomicLong reportedHamMessageCount;
        private final AtomicLong errorCount;
        private final Integer messagesPerSecond;
        private final Optional<Long> period;
        private final Double samplingProbability;

        public Context(RunningOptions runningOptions) {
            this.hamMessageCount = new AtomicLong();
            this.reportedHamMessageCount = new AtomicLong();
            this.errorCount = new AtomicLong();
            this.messagesPerSecond = runningOptions.messagesPerSecond;
            this.period = runningOptions.periodInSecond;
            this.samplingProbability = runningOptions.samplingProbability;
        }

        public void incrementHamMessageCount() {
            hamMessageCount.incrementAndGet();
        }

        public void incrementReportedHamMessageCount(int count) {
            reportedHamMessageCount.addAndGet(count);
        }

        public void incrementErrorCount() {
            errorCount.incrementAndGet();
        }

        public Snapshot snapshot() {
            return Snapshot.builder()
                .hamMessageCount(hamMessageCount.get())
                .reportedHamMessageCount(reportedHamMessageCount.get())
                .errorCount(errorCount.get())
                .messagesPerSecond(messagesPerSecond)
                .period(period)
                .samplingProbability(samplingProbability)
                .build();
        }
    }

    private final GetMailboxMessagesService messagesService;
    private final RSpamDHttpClient rSpamDHttpClient;
    private final RunningOptions runningOptions;
    private final Context context;
    private final Clock clock;

    public FeedHamToRSpamDTask(MailboxManager mailboxManager, UsersRepository usersRepository, MessageIdManager messageIdManager, MailboxSessionMapperFactory mapperFactory,
                               RSpamDHttpClient rSpamDHttpClient, RunningOptions runningOptions, Clock clock) {
        this.runningOptions = runningOptions;
        this.messagesService = new GetMailboxMessagesService(mailboxManager, usersRepository, mapperFactory, messageIdManager);
        this.rSpamDHttpClient = rSpamDHttpClient;
        this.context = new Context(runningOptions);
        this.clock = clock;
    }

    @Override
    public Result run() {
        Optional<Date> afterDate = runningOptions.periodInSecond.map(periodInSecond -> Date.from(clock.instant().minusSeconds(periodInSecond)));
        try {
            return messagesService.getHamMessagesOfAllUser(afterDate, runningOptions.getSamplingProbability(), context)
                .transform(ReactorUtils.<MessageResult, Result>throttle()
                    .elements(runningOptions.messagesPerSecond)
                    .per(Duration.ofSeconds(1))
                    .forOperation(messageResult -> Mono.fromSupplier(Throwing.supplier(() -> rSpamDHttpClient.reportAsHam(messageResult.getFullContent().getInputStream())))
                        .then(Mono.fromCallable(() -> {
                            context.incrementReportedHamMessageCount(1);
                            return Result.COMPLETED;
                        }))
                        .onErrorResume(error -> {
                            LOGGER.error("Error when report ham message to RSpamD", error);
                            context.incrementErrorCount();
                            return Mono.just(Result.PARTIAL);
                        })))
                .reduce(Task::combine)
                .switchIfEmpty(Mono.just(Result.COMPLETED))
                .block();
        } catch (UsersRepositoryException e) {
            LOGGER.error("Error while accessing users from repository", e);
            return Result.PARTIAL;
        }
    }

    @Override
    public TaskType type() {
        return TASK_TYPE;
    }

    @Override
    public Optional<TaskExecutionDetails.AdditionalInformation> details() {
        return Optional.of(AdditionalInformation.from(context));
    }

    @VisibleForTesting
    public Context.Snapshot snapshot() {
        return context.snapshot();
    }
}
