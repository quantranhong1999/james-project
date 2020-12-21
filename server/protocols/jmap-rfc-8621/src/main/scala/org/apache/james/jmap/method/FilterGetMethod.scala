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

package org.apache.james.jmap.method

import com.google.common.collect.ImmutableList
import eu.timepit.refined.auto._
import org.apache.james.jmap.api.filtering.{FilteringManagement, Rule => JavaRule}
import org.apache.james.jmap.core.CapabilityIdentifier.{CapabilityIdentifier, JMAP_FILTER}
import org.apache.james.jmap.core.Invocation
import org.apache.james.jmap.core.Invocation.{Arguments, MethodName}
import org.apache.james.jmap.filter.{Action, AppendIn, Comparator, Condition, Field, Filter, FilterGetNotFound, FilterGetRequest, FilterGetResponse, Rule}
import org.apache.james.jmap.json.{FilterSerializer, ResponseSerializer}
import org.apache.james.jmap.mail.Name
import org.apache.james.jmap.routes.SessionSupplier
import org.apache.james.mailbox.MailboxSession
import org.apache.james.mailbox.model.MailboxId
import org.apache.james.metrics.api.MetricFactory
import org.reactivestreams.Publisher
import play.api.libs.json.{JsError, JsObject, JsSuccess}
import reactor.core.scala.publisher.SFlux

import javax.inject.Inject

class FilterGetMethod @Inject()(val metricFactory: MetricFactory,
                                val sessionSupplier: SessionSupplier,
                                val mailboxIdFactory: MailboxId.Factory,
                                filteringManagement: FilteringManagement) extends MethodRequiringAccountId[FilterGetRequest] {

  override val methodName: Invocation.MethodName = MethodName("Filter/get")
  override val requiredCapabilities: Set[CapabilityIdentifier] = Set(JMAP_FILTER)

  override def doProcess(capabilities: Set[CapabilityIdentifier], invocation: InvocationWithContext, mailboxSession: MailboxSession, request: FilterGetRequest): Publisher[InvocationWithContext] = {
      SFlux.fromPublisher(filteringManagement.listRulesForUser(mailboxSession.getUser))
      .reduce[List[Rule]](List(), (listRule, rule) => {
        listRule ++ List(parseRuleFromJavaToScala(rule))
      })
      .map[Filter](listRule => {
        Filter("singleton", listRule)
      })
      .map[FilterGetResponse](filter => request.ids match {
        case None => FilterGetResponse(request.accountId, List(filter), FilterGetNotFound(List()))
        case Some(ids) => if(ids.value.contains("singleton")) {
          FilterGetResponse(request.accountId, List(filter), FilterGetNotFound(ids.value.filterNot(id => id.equals("singleton"))))
        } else {
          FilterGetResponse(request.accountId, List(), FilterGetNotFound(ids.value))
        }
      })
      .map(response => InvocationWithContext(
        invocation = Invocation(
          methodName = methodName,
          arguments = Arguments(FilterSerializer.serialize(response).as[JsObject]),
          methodCallId = invocation.invocation.methodCallId),
        processingContext = invocation.processingContext))
  }

  override def getRequest(mailboxSession: MailboxSession, invocation: Invocation): Either[Exception, FilterGetRequest] =
    FilterSerializer.deserializeFilterGetRequest(invocation.arguments.value) match {
      case JsSuccess(filterGetRequest, _) => Right(filterGetRequest)
      case errors: JsError => Left(new IllegalArgumentException(ResponseSerializer.serialize(errors).toString))
    }

  private def parseRuleFromJavaToScala(rule: JavaRule): Rule = {
    Rule(Name(rule.getName),
      Condition(Field(rule.getCondition.getField.asString()), Comparator(rule.getCondition.getComparator.asString()), rule.getCondition.getValue),
      Action(AppendIn(parseMailboxIds(rule.getAction.getAppendInMailboxes.getMailboxIds))))
  }

  private def parseMailboxIds(mailboxIds: ImmutableList[String]) : List[MailboxId] = {
    List(mailboxIdFactory.fromString(mailboxIds.get(0)))
  }

}
