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

import java.io.IOException;

import org.apache.james.json.DTOModule;
import org.apache.james.server.task.json.dto.AdditionalInformationDTO;
import org.apache.james.server.task.json.dto.AdditionalInformationDTOModule;
import org.apache.james.webadmin.data.jmap.PopulateEmailQueryViewTask.AdditionalInformation.MailboxCounters;
import org.apache.james.webadmin.data.jmap.PopulateEmailQueryViewTask.AdditionalInformation.UserCounters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

// Details are written with the DTOV2 format, counting mailboxes. The DTOV1 format, counting users, is the one of the
// runs made before the task walked mailboxes: it is still read, and rendered as is.
@JsonDeserialize(using = PopulateEmailQueryViewTaskAdditionalInformationDTO.Deserializer.class)
public sealed interface PopulateEmailQueryViewTaskAdditionalInformationDTO extends AdditionalInformationDTO
    permits PopulateEmailQueryViewTaskAdditionalInformationDTOV1, PopulateEmailQueryViewTaskAdditionalInformationDTOV2 {

    // Both formats share the type name: their counters tell them apart
    class Deserializer extends StdDeserializer<PopulateEmailQueryViewTaskAdditionalInformationDTO> {
        protected Deserializer() {
            super(PopulateEmailQueryViewTaskAdditionalInformationDTO.class);
        }

        @Override
        public PopulateEmailQueryViewTaskAdditionalInformationDTO deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
            JsonNode json = context.readTree(jsonParser);
            if (json.has("processedUserCount")) {
                return context.readTreeAsValue(json, PopulateEmailQueryViewTaskAdditionalInformationDTOV1.class);
            }
            return context.readTreeAsValue(json, PopulateEmailQueryViewTaskAdditionalInformationDTOV2.class);
        }
    }

    static AdditionalInformationDTOModule<PopulateEmailQueryViewTask.AdditionalInformation, PopulateEmailQueryViewTaskAdditionalInformationDTO> module() {
        return DTOModule.forDomainObject(PopulateEmailQueryViewTask.AdditionalInformation.class)
            .convertToDTO(PopulateEmailQueryViewTaskAdditionalInformationDTO.class)
            .toDomainObjectConverter(PopulateEmailQueryViewTaskAdditionalInformationDTO::toDomainObject)
            .toDTOConverter(PopulateEmailQueryViewTaskAdditionalInformationDTO::toDTO)
            .typeName(PopulateEmailQueryViewTask.TASK_TYPE.asString())
            .withFactory(AdditionalInformationDTOModule::new);
    }

    private static PopulateEmailQueryViewTaskAdditionalInformationDTO toDTO(PopulateEmailQueryViewTask.AdditionalInformation details, String type) {
        return switch (details.getCounters()) {
            case MailboxCounters counters -> PopulateEmailQueryViewTaskAdditionalInformationDTOV2.from(details, counters, type);
            case UserCounters counters -> PopulateEmailQueryViewTaskAdditionalInformationDTOV1.from(details, counters, type);
        };
    }

    PopulateEmailQueryViewTask.AdditionalInformation toDomainObject();
}
