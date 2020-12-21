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

package org.apache.james.jmap.json

import org.apache.james.jmap.filter.{Action, AppendIn, Comparator, Condition, Field, Filter, FilterGetIds, FilterGetNotFound, FilterGetRequest, FilterGetResponse, Rule}
import org.apache.james.jmap.mail.Name
import org.apache.james.mailbox.model.MailboxId
import play.api.libs.json.{JsResult, JsString, JsValue, Json, Reads, Writes}

object FilterSerializer {

  implicit val filterGetIds: Reads[FilterGetIds] = Json.valueReads[FilterGetIds]
  implicit val filterGetRequestReads: Reads[FilterGetRequest] = Json.reads[FilterGetRequest]

  implicit val mailboxIdWrites: Writes[MailboxId] = mailboxId => JsString(mailboxId.serialize)
  implicit val appendIn: Writes[AppendIn] = Json.writes[AppendIn]
  implicit val actionWrites: Writes[Action] = Json.writes[Action]
  implicit val comparatorWrites: Writes[Comparator] = Json.valueWrites[Comparator]
  implicit val fieldWrites: Writes[Field] = Json.valueWrites[Field]
  implicit val conditionWrites: Writes[Condition] = Json.writes[Condition]
  implicit val nameWrites: Writes[Name] = Json.valueWrites[Name]
  implicit val ruleWrites: Writes[Rule] = Json.writes[Rule]
  implicit val filterWrites: Writes[Filter] = Json.writes[Filter]
  implicit val notFoundWrites: Writes[FilterGetNotFound] = Json.valueWrites[FilterGetNotFound]
  implicit val filterGetResponseWrites: Writes[FilterGetResponse] = Json.writes[FilterGetResponse]

  def serialize(response: FilterGetResponse): JsValue = Json.toJson(response)

  def deserializeFilterGetRequest(input: JsValue): JsResult[FilterGetRequest] = Json.fromJson[FilterGetRequest](input)
}
