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

package org.apache.james.jmap.rfc8621.contract

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

import io.netty.handler.codec.http.HttpHeaderNames.ACCEPT
import io.restassured.RestAssured.{`given`, requestSpecification}
import io.restassured.builder.ResponseSpecBuilder
import io.restassured.http.ContentType.JSON
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.apache.http.HttpStatus.SC_OK
import org.apache.james.GuiceJamesServer
import org.apache.james.jmap.http.UserCredential
import org.apache.james.jmap.rfc8621.contract.Fixture.{ACCEPT_RFC8621_VERSION_HEADER, ACCOUNT_ID, ANDRE, ANDRE_ACCOUNT_ID, ANDRE_PASSWORD, BOB, BOB_PASSWORD, DOMAIN, authScheme, baseRequestSpecBuilder}
import org.apache.james.mailbox.DefaultMailboxes
import org.apache.james.mailbox.MessageManager.AppendCommand
import org.apache.james.mailbox.model.{MailboxId, MailboxPath, MessageId}
import org.apache.james.mime4j.dom.Message
import org.apache.james.modules.MailboxProbeImpl
import org.apache.james.utils.DataProbeImpl
import org.awaitility.Awaitility
import org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS
import org.junit.jupiter.api.{BeforeEach, Test}

trait EmailSubmissionSetMethodFutureReleaseContract {
  private lazy val slowPacedPollInterval = ONE_HUNDRED_MILLISECONDS
  private lazy val calmlyAwait = Awaitility.`with`
    .pollInterval(slowPacedPollInterval)
    .and.`with`.pollDelay(slowPacedPollInterval)
    .await
  private lazy val awaitAtMostTenSeconds = calmlyAwait.atMost(10, TimeUnit.SECONDS)

  @BeforeEach
  def setUp(server: GuiceJamesServer): Unit = {
    server.getProbe(classOf[DataProbeImpl])
      .fluent
      .addDomain(DOMAIN.asString)
      .addUser(BOB.asString, BOB_PASSWORD)
      .addUser(ANDRE.asString, ANDRE_PASSWORD)

    requestSpecification = baseRequestSpecBuilder(server)
      .setAuth(authScheme(UserCredential(BOB, BOB_PASSWORD)))
      .build
  }

  def randomMessageId: MessageId

  @Test
  def emailSubmissionSetCreateShouldSendMailSuccessfully(server: GuiceJamesServer): Unit = {
    val message: Message = Message.Builder
      .of
      .setSubject("test")
      .setSender(BOB.asString)
      .setFrom(BOB.asString)
      .setTo(ANDRE.asString)
      .setBody("testmail", StandardCharsets.UTF_8)
      .build

    val bobDraftsPath = MailboxPath.forUser(BOB, DefaultMailboxes.DRAFTS)
    server.getProbe(classOf[MailboxProbeImpl]).createMailbox(bobDraftsPath)
    val messageId: MessageId = server.getProbe(classOf[MailboxProbeImpl]).appendMessage(BOB.asString(), bobDraftsPath, AppendCommand.builder()
      .build(message))
      .getMessageId

    val andreInboxPath = MailboxPath.inbox(ANDRE)
    val andreInboxId: MailboxId = server.getProbe(classOf[MailboxProbeImpl]).createMailbox(andreInboxPath)

    val requestBob =
      s"""{
         |  "using": ["urn:ietf:params:jmap:core", "urn:ietf:params:jmap:mail", "urn:ietf:params:jmap:submission"],
         |  "methodCalls": [
         |     ["EmailSubmission/set", {
         |       "accountId": "$ACCOUNT_ID",
         |       "create": {
         |         "k1490": {
         |           "emailId": "${messageId.serialize}",
         |           "envelope": {
         |             "mailFrom": {
         |                "email": "${BOB.asString}",
         |                "parameters": {}
         |                },
         |             "rcptTo": [{"email": "${ANDRE.asString}"}]
         |           }
         |         }
         |    }
         |  }, "c1"]]
         |}""".stripMargin

    `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(requestBob)
    .when
      .post.prettyPeek()
    .`then`
      .statusCode(SC_OK)

    val requestAndre =
      s"""{
         |  "using": ["urn:ietf:params:jmap:core","urn:ietf:params:jmap:mail", "urn:ietf:params:jmap:submission"],
         |  "methodCalls": [[
         |    "Email/query",
         |    {
         |      "accountId": "$ANDRE_ACCOUNT_ID",
         |      "filter": {"inMailbox": "${andreInboxId.serialize}"}
         |    },
         |    "c1"]]
         |}""".stripMargin

    awaitAtMostTenSeconds.untilAsserted { () =>
      val response = `given`(
        baseRequestSpecBuilder(server)
          .setAuth(authScheme(UserCredential(ANDRE, ANDRE_PASSWORD)))
          .addHeader(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
          .setBody(requestAndre)
          .build, new ResponseSpecBuilder().build)
        .post
      .`then`
        .statusCode(SC_OK)
        .contentType(JSON)
        .extract
        .body
        .asString

      assertThatJson(response)
        .inPath("methodResponses[0][1].ids")
        .isArray
        .hasSize(1)
    }
  }
}
