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

package org.apache.james.jmap.pushsubscription

import java.time.ZonedDateTime

import org.apache.james.core.Username
import org.apache.james.jmap.change.TypeName
import org.apache.james.jmap.mail.{PushSubscription, PushSubscriptionCreationRequest, PushSubscriptionId}
import org.reactivestreams.Publisher

trait PushSubscriptionRepository {
  def save(username: Username, pushSubscriptionCreationRequest: PushSubscriptionCreationRequest): Publisher[PushSubscriptionId]

  def updateExpireTime(username: Username, id: PushSubscriptionId, newExpire: ZonedDateTime): Publisher[Unit]

  def updateTypes(username: Username, id: PushSubscriptionId, types: Seq[TypeName]): Publisher[Unit]

  def revoke(username: Username, id: PushSubscriptionId): Publisher[Unit]

  def get(username: Username, ids: Seq[PushSubscriptionId]): Publisher[PushSubscription]

  def list(username: Username): Publisher[PushSubscription]

  def validateVerificationCode(username: Username, id: PushSubscriptionId): Publisher[Unit]
}
