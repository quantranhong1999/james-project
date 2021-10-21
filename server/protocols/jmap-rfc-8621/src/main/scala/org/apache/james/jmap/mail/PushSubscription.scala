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

package org.apache.james.jmap.mail

import java.time.ZonedDateTime
import java.util.UUID

import org.apache.james.jmap.change.TypeName

case class PushSubscriptionId(value: UUID)

case class DeviceClientId(value: String) extends AnyVal

case class VerificationCode(value: String) extends AnyVal

case class PushSubscriptionServerURL(value: String) extends AnyVal

case class PushSubscriptionExpiredTime(value: ZonedDateTime)

case class PushSubscriptionKeys(p256dh: String, auth: String)

case class PushSubscriptionCreationRequest(deviceClientId: DeviceClientId,
                                           url: PushSubscriptionServerURL,
                                           keys: Option[PushSubscriptionKeys],
                                           expires: Option[PushSubscriptionExpiredTime],
                                           types: Seq[TypeName])

case class PushSubscription(id: PushSubscriptionId,
                            deviceClientId: DeviceClientId,
                            url: PushSubscriptionServerURL,
                            keys: Option[PushSubscriptionKeys],
                            verificationCode: VerificationCode,
                            validated: Boolean,
                            expires: PushSubscriptionExpiredTime,
                            types: Seq[TypeName])
