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

package org.apache.james.jmap.filter

import org.apache.james.jmap.core.AccountId
import org.apache.james.jmap.core.Id.Id
import org.apache.james.jmap.mail.Name
import org.apache.james.jmap.method.WithAccountId
import org.apache.james.mailbox.model.MailboxId

case class FilterGetRequest(accountId: AccountId,
                            ids: Option[FilterGetIds]) extends WithAccountId

case class FilterGetResponse(accountId: AccountId,
                             list: List[Filter],
                             notFound: FilterGetNotFound)

case class FilterGetIds(value: List[String])

case class Condition(field: Field, comparator: Comparator, value: String)

case class Field(string: String)

case class Comparator(string: String)

case class AppendIn(mailboxIds: List[MailboxId])

case class Action(appendIn: AppendIn)

case class Rule(name: Name, condition: Condition, action: Action)

case class Filter(id: Id, rules: List[Rule])

case class FilterGetNotFound(value: List[String]) {
  def merge(other: FilterGetNotFound): FilterGetNotFound = FilterGetNotFound(this.value ++ other.value)
}