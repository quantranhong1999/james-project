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

import java.time.Instant;
import java.util.Optional;

import org.apache.james.webadmin.data.jmap.PopulateEmailQueryViewTask.AdditionalInformation.MailboxCounters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.annotations.VisibleForTesting;

// Format of the runs that walk mailboxes
// Once picked by the Deserializer of the interface, parsed directly: not looked up again by type name
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@JsonDeserialize(as = PopulateEmailQueryViewTaskAdditionalInformationDTOV2.class)
public final class PopulateEmailQueryViewTaskAdditionalInformationDTOV2 implements PopulateEmailQueryViewTaskAdditionalInformationDTO {
    static PopulateEmailQueryViewTaskAdditionalInformationDTOV2 from(PopulateEmailQueryViewTask.AdditionalInformation details, MailboxCounters counters, String type) {
        return new PopulateEmailQueryViewTaskAdditionalInformationDTOV2(
            type,
            details.timestamp(),
            counters.processedMailboxCount(),
            details.getProcessedMessageCount(),
            counters.failedMailboxCount(),
            details.getFailedMessageCount(),
            Optional.of(RunningOptionsDTO.asDTO(details.getRunningOptions())));
    }

    private final String type;
    private final Instant timestamp;
    private final long processedMailboxCount;
    private final long processedMessageCount;
    private final long failedMailboxCount;
    private final long failedMessageCount;
    private final Optional<RunningOptionsDTO> runningOptions;

    @VisibleForTesting
    PopulateEmailQueryViewTaskAdditionalInformationDTOV2(@JsonProperty("type") String type,
                                                         @JsonProperty("timestamp") Instant timestamp,
                                                         @JsonProperty("processedMailboxCount") long processedMailboxCount,
                                                         @JsonProperty("processedMessageCount") long processedMessageCount,
                                                         @JsonProperty("failedMailboxCount") long failedMailboxCount,
                                                         @JsonProperty("failedMessageCount") long failedMessageCount,
                                                         @JsonProperty("runningOptions") Optional<RunningOptionsDTO> runningOptions) {
        this.type = type;
        this.timestamp = timestamp;
        this.processedMailboxCount = processedMailboxCount;
        this.processedMessageCount = processedMessageCount;
        this.failedMailboxCount = failedMailboxCount;
        this.failedMessageCount = failedMessageCount;
        this.runningOptions = runningOptions;
    }

    @Override
    public PopulateEmailQueryViewTask.AdditionalInformation toDomainObject() {
        return new PopulateEmailQueryViewTask.AdditionalInformation(
            runningOptions
                .map(RunningOptionsDTO::asDomainObject)
                .orElse(RunningOptions.DEFAULT),
            new MailboxCounters(processedMailboxCount, failedMailboxCount),
            processedMessageCount,
            failedMessageCount,
            timestamp);
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    public long getProcessedMailboxCount() {
        return processedMailboxCount;
    }

    public long getProcessedMessageCount() {
        return processedMessageCount;
    }

    public long getFailedMailboxCount() {
        return failedMailboxCount;
    }

    public long getFailedMessageCount() {
        return failedMessageCount;
    }

    public Optional<RunningOptionsDTO> getRunningOptions() {
        return runningOptions;
    }
}
