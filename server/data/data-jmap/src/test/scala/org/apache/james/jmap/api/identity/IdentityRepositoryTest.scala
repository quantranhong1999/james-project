/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.jmap.api.identity

import org.apache.james.core.{MailAddress, Username}
import org.apache.james.jmap.api.identity.IdentityRepositoryTest.{BOB, CREATION_REQUEST, IDENTITY1, UPDATE_REQUEST}
import org.apache.james.jmap.api.model.{EmailAddress, EmailerName, ForbiddenSendFromException, HtmlSignature, Identity, IdentityId, IdentityName, MayDeleteIdentity, TextSignature}
import org.apache.james.jmap.memory.identity.MemoryCustomIdentityDAO
import org.assertj.core.api.Assertions.{assertThat, assertThatCode, assertThatThrownBy}
import org.junit.jupiter.api.{BeforeEach, Test}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import reactor.core.scala.publisher.{SFlux, SMono}

import scala.jdk.CollectionConverters._

object IdentityRepositoryTest {
  val BOB: Username = Username.of("bob@domain.tld")

  val CREATION_REQUEST: IdentityCreationRequest = IdentityCreationRequest(name = Some(IdentityName("Bob (custom address)")),
    email = BOB.asMailAddress(),
    replyTo = Some(List(EmailAddress(Some(EmailerName("My Boss")), new MailAddress("boss@domain.tld")))),
    bcc = Some(List(EmailAddress(Some(EmailerName("My Boss 2")), new MailAddress("boss2@domain.tld")))),
    textSignature = Some(TextSignature("text signature")),
    htmlSignature = Some(HtmlSignature("html signature")))

  val UPDATE_REQUEST: IdentityUpdateRequest = IdentityUpdateRequest(
    name = Some(IdentityNameUpdate(IdentityName("Bob (new name)"))),
    replyTo = Some(IdentityReplyToUpdate(Some(List(EmailAddress(Some(EmailerName("My Boss (updated)")), new MailAddress("boss-updated@domain.tld")))))),
    bcc = Some(IdentityBccUpdate(Some(List(EmailAddress(Some(EmailerName("My Boss 2 (updated)")), new MailAddress("boss-updated-2@domain.tld")))))),
    textSignature = Some(IdentityTextSignatureUpdate(TextSignature("text 2 signature"))),
    htmlSignature = Some(IdentityHtmlSignatureUpdate(HtmlSignature("html 2 signature"))))

  val IDENTITY1: Identity = Identity(id = IdentityId.generate,
    name = IdentityName(""),
    email = BOB.asMailAddress(),
    replyTo = Some(List(EmailAddress(Some(EmailerName("My Boss 1")), new MailAddress("boss1@domain.tld")))),
    bcc = Some(List(EmailAddress(Some(EmailerName("My Boss bcc 1")), new MailAddress("boss_bcc_1@domain.tld")))),
    textSignature = TextSignature(""),
    htmlSignature = HtmlSignature(""),
    mayDelete = MayDeleteIdentity(false))

  val IDENTITY2: Identity = Identity(id = IdentityId.generate,
    name = IdentityName(""),
    email = BOB.asMailAddress(),
    replyTo = Some(List(EmailAddress(Some(EmailerName("My Boss")), new MailAddress("boss@domain.tld")))),
    bcc = Some(List(EmailAddress(Some(EmailerName("My Boss 2")), new MailAddress("boss2@domain.tld")))),
    textSignature = TextSignature("text signature"),
    htmlSignature = HtmlSignature("html signature"),
    mayDelete = MayDeleteIdentity(true))

}

class IdentityRepositoryTest {

  var testee: IdentityRepository = _
  var identityFactory: DefaultIdentitySupplier = _
  var customIdentityDAO: CustomIdentityDAO = _


  @BeforeEach
  def setUp(): Unit = {
    customIdentityDAO = new MemoryCustomIdentityDAO()
    identityFactory = mock(classOf[DefaultIdentitySupplier])
    testee = new IdentityRepository(customIdentityDAO, identityFactory)
  }

  @Test
  def saveShouldSuccess(): Unit = {
    when(identityFactory.userCanSendFrom(any(), any())).thenReturn(true)
    assertThatCode(() => SMono.fromPublisher(testee.save(BOB, CREATION_REQUEST)).block())
      .doesNotThrowAnyException()
  }

  @Test
  def saveShouldFailWhenUserCanNotSendFrom(): Unit = {
    when(identityFactory.userCanSendFrom(any(), any())).thenReturn(false)
    assertThatThrownBy(() => SMono.fromPublisher(testee.save(BOB, CREATION_REQUEST)).block())
      .isInstanceOf(classOf[ForbiddenSendFromException])
  }

  @Test
  def listShouldReturnCustomAndServerSetEntries(): Unit = {
    SMono.fromPublisher(customIdentityDAO.save(BOB, CREATION_REQUEST)).block()
    when(identityFactory.listIdentities(BOB)).thenReturn(List(IDENTITY1))

    assertThat(SFlux.fromPublisher(testee.list(BOB)).collectSeq().block().asJava)
      .containsExactlyInAnyOrder(IDENTITY1, SFlux(customIdentityDAO.list(BOB)).blockFirst().get)
  }

  @Test
  def listShouldReturnCustomEntryWhenIdExistsInBothCustomAndServerSet(): Unit = {
    SMono.fromPublisher(customIdentityDAO.save(BOB, CREATION_REQUEST)).block()
    val identity: Identity = SFlux(customIdentityDAO.list(BOB)).blockFirst().get
    val differentIdentityWithSameId: Identity = identity.copy(name = IdentityName("different name"))
    when(identityFactory.listIdentities(BOB)).thenReturn(List(differentIdentityWithSameId))

    assertThat(SFlux.fromPublisher(testee.list(BOB)).collectSeq().block().asJava)
      .containsExactlyInAnyOrder(identity)
  }

  @Test
  def updateShouldFailWhenIdNotFoundInBothCustomAndServerSetDAO(): Unit = {
    when(identityFactory.listIdentities(BOB)).thenReturn(List())

    assertThatThrownBy(() => SMono.fromPublisher(testee.update(BOB, IdentityId.generate, UPDATE_REQUEST)).block())
      .isInstanceOf(classOf[IdentityNotFoundException])
  }

  @Test
  def updateShouldSuccessWhenCustomNotFoundAndServerSetExists(): Unit = {
    when(identityFactory.listIdentities(BOB)).thenReturn(List(IDENTITY1))

    assertThatCode(() => SMono.fromPublisher(testee.update(BOB, IDENTITY1.id, UPDATE_REQUEST)).block())
      .doesNotThrowAnyException()
  }

  @Test
  def updateShouldSuccessWhenCustomExists(): Unit = {
    when(identityFactory.listIdentities(BOB)).thenReturn(List())
    SMono.fromPublisher(customIdentityDAO.save(BOB, CREATION_REQUEST)).block()
    val identity: Identity = SFlux(customIdentityDAO.list(BOB)).blockFirst().get

    assertThatCode(() => SMono.fromPublisher(testee.update(BOB, identity.id, UPDATE_REQUEST)).block())
      .doesNotThrowAnyException()
  }

}
