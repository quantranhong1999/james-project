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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.apache.james.JsonSerializationVerifier;
import org.apache.james.json.JsonGenericSerializer;
import org.apache.james.util.ClassLoaderUtils;
import org.apache.james.webadmin.data.jmap.PopulateEmailQueryViewTask.AdditionalInformation.MailboxCounters;
import org.apache.james.webadmin.data.jmap.PopulateEmailQueryViewTask.AdditionalInformation.UserCounters;
import org.junit.jupiter.api.Test;

class PopulateEmailQueryViewProjectionItemsTaskAdditionalInformationDTOTest {
    private static final Instant INSTANT = Instant.parse("2007-12-03T10:15:30.00Z");
    private static final PopulateEmailQueryViewTask.AdditionalInformation DOMAIN_OBJECT = new PopulateEmailQueryViewTask.AdditionalInformation(
        RunningOptions.withMessageRatePerSecond(20),
        new MailboxCounters(1, 3),
        2,
        4,
        INSTANT);
    private static final PopulateEmailQueryViewTask.AdditionalInformation LEGACY_DOMAIN_OBJECT = new PopulateEmailQueryViewTask.AdditionalInformation(
        RunningOptions.withMessageRatePerSecond(20),
        new UserCounters(1, 3),
        2,
        4,
        INSTANT);

    @Test
    void shouldMatchJsonSerializationContract() throws Exception {
        JsonSerializationVerifier.dtoModule(PopulateEmailQueryViewTaskAdditionalInformationDTO.module())
            .bean(DOMAIN_OBJECT)
            .json(ClassLoaderUtils.getSystemResourceAsString("json/populateAll.additionalInformation.json"))
            .testCase(LEGACY_DOMAIN_OBJECT, ClassLoaderUtils.getSystemResourceAsString("json/populateAll.additionalInformation.legacy.json"))
            .verify();
    }

    @Test
    void shouldRejectDetailsMixingUserAndMailboxCounters() {
        JsonGenericSerializer<PopulateEmailQueryViewTask.AdditionalInformation, PopulateEmailQueryViewTaskAdditionalInformationDTO> serializer = JsonGenericSerializer
            .forModules(PopulateEmailQueryViewTaskAdditionalInformationDTO.module())
            .withoutNestedType();

        assertThatThrownBy(() -> serializer.deserialize(ClassLoaderUtils.getSystemResourceAsString("json/populateAll.additionalInformation.mixedCounters.json")))
            .isInstanceOf(JsonGenericSerializer.InvalidTypeException.class);
    }
}
