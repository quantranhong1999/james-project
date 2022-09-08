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

package org.apache.james.imapserver.netty;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.concurrent.ExecutionException;

import org.apache.james.metrics.api.GaugeRegistry;
import org.apache.james.metrics.dropwizard.DropWizardGaugeRegistry;
import org.apache.james.util.ReactorUtils;
import org.apache.james.util.concurrency.ConcurrentTestRunner;
import org.junit.jupiter.api.Test;

import com.codahale.metrics.MetricRegistry;

import reactor.core.publisher.Mono;

public class ReactiveThrottlerTest {
    @Test
    void test() throws ExecutionException, InterruptedException {
        GaugeRegistry gaugeRegistry = new DropWizardGaugeRegistry(new MetricRegistry());
        int maxConcurrentRequests = 10;
        int maxQueueSize = 0;
        ReactiveThrottler reactiveThrottler = new ReactiveThrottler(gaugeRegistry, maxConcurrentRequests, maxQueueSize);

        ConcurrentTestRunner.builder()
            .operation(((threadNumber, step) -> Mono.from(reactiveThrottler.throttle(longExecutionTask(threadNumber, step)))
                .subscribeOn(ReactorUtils.BLOCKING_CALL_WRAPPER)
                .subscribe()))
            .threadCount(10)
            .operationCount(1)
            .run();

        Thread.sleep(1000);

//        for (int i = 0; i < maxConcurrentRequests + maxQueueSize; i++) {
//            Mono.from(reactiveThrottler.throttle(longExecutionTask(0, 0)))
//                .subscribeOn(ReactorUtils.BLOCKING_CALL_WRAPPER)
//                .subscribe();
//        }

        // next 11th query should be fail
        assertThatThrownBy(() -> Mono.from(reactiveThrottler.throttle(Mono.empty()))
            .subscribeOn(ReactorUtils.BLOCKING_CALL_WRAPPER)
            .block());
    }

    private Mono<Void> longExecutionTask(int threadNumber, int step) {
        return Mono.just(0)
            .then(Mono.fromCallable(() -> {
                System.out.println(String.format("Thread %s step %d starting at %s", Thread.currentThread().getName(), step, Instant.now()));
                Thread.sleep(5000);
                System.out.println(String.format("Thread %s step %d finished at %s", Thread.currentThread().getName(), step, Instant.now()));
                return null;
            }));
    }

}
