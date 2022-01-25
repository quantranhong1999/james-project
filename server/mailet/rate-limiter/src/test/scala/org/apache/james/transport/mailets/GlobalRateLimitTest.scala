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

package org.apache.james.transport.mailets

import org.apache.james.core.MailAddress
import org.apache.james.rate.limiter.memory.MemoryRateLimiterFactoryProvider
import org.apache.mailet.base.test.{FakeMail, FakeMailetConfig}
import org.apache.mailet.{Mail, MailetConfig}
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test

import scala.jdk.CollectionConverters._

class GlobalRateLimitTest {
  def testee(mailetConfig: MailetConfig): GlobalRateLimit = {
    val mailet = new GlobalRateLimit(new MemoryRateLimiterFactoryProvider())
    mailet.init(mailetConfig)
    mailet
  }

  @Test
  def rateLimitingShouldBeAppliedGlobally(): Unit = {
    val mailet = testee(FakeMailetConfig.builder()
      .mailetName("GlobalRateLimit")
      .setProperty("duration", "20s")
      .setProperty("count", "1")
      .build())

    val mail1: Mail = FakeMail.builder()
      .name("mail1")
      .sender("sender1@domain.tld")
      .recipients("rcpt1@linagora.com", "rcpt2@linagora.com")
      .state("transport")
      .build()

    val mail2: Mail = FakeMail.builder()
      .name("mail2")
      .sender("sender2@domain.tld")
      .recipients("rcpt1@linagora.com", "rcpt3@linagora.com", "rcpt4@linagora.com")
      .state("transport")
      .build()

    mailet.service(mail1)
    mailet.service(mail2)

    SoftAssertions.assertSoftly(softly => {
      softly.assertThat(mail1.getState).isEqualTo("transport")
      softly.assertThat(mail2.getState).isEqualTo("error")
    })
  }

  @Test
  def rateLimitedEmailsShouldFlowToTheIntendedProcessor(): Unit = {
    val mailet = testee(FakeMailetConfig.builder()
      .mailetName("GlobalRateLimit")
      .setProperty("duration", "20s")
      .setProperty("count", "1")
      .setProperty("exceededProcessor", "tooMuchMails")
      .build())

    val mail1: Mail = FakeMail.builder()
      .name("mail1")
      .sender("sender1@domain.tld")
      .recipients("rcpt1@linagora.com", "rcpt2@linagora.com")
      .state("transport")
      .build()

    val mail2: Mail = FakeMail.builder()
      .name("mail2")
      .sender("sender2@domain.tld")
      .recipients("rcpt1@linagora.com", "rcpt3@linagora.com", "rcpt4@linagora.com")
      .state("transport")
      .build()

    mailet.service(mail1)
    mailet.service(mail2)

    SoftAssertions.assertSoftly(softly => {
      softly.assertThat(mail1.getState).isEqualTo("transport")
      softly.assertThat(mail2.getState).isEqualTo("tooMuchMails")
    })
  }

  @Test
  def shouldRateLimitCountOfEmails(): Unit = {
    val mailet = testee(FakeMailetConfig.builder()
      .mailetName("GlobalRateLimit")
      .setProperty("duration", "20s")
      .setProperty("count", "2")
      .build())

    val mail1: Mail = FakeMail.builder()
      .name("mail1")
      .sender("sender1@domain.tld")
      .recipients("rcpt1@linagora.com", "rcpt2@linagora.com")
      .state("transport")
      .build()

    val mail2: Mail = FakeMail.builder()
      .name("mail2")
      .sender("sender2@domain.tld")
      .recipients("rcpt1@linagora.com", "rcpt3@linagora.com", "rcpt4@linagora.com")
      .state("transport")
      .build()

    val mail3: Mail = FakeMail.builder()
      .name("mail3")
      .sender("sender3@domain.tld")
      .recipients("rcpt1@linagora.com", "rcpt3@linagora.com", "rcpt4@linagora.com")
      .state("transport")
      .build()

    mailet.service(mail1)
    mailet.service(mail2)
    mailet.service(mail3)

    SoftAssertions.assertSoftly(softly => {
      softly.assertThat(mail1.getState).isEqualTo("transport")
      softly.assertThat(mail2.getState).isEqualTo("transport")
      softly.assertThat(mail3.getState).isEqualTo("error")
    })
  }

  @Test
  def shouldRateLimitRecipientsOfEmails(): Unit = {
    val mailet = testee(FakeMailetConfig.builder()
      .mailetName("GlobalRateLimit")
      .setProperty("duration", "20s")
      .setProperty("recipients", "4")
      .build())

    val mail1: Mail = FakeMail.builder()
      .name("mail1")
      .sender("sender1@domain.tld")
      .recipients("rcpt1@linagora.com", "rcpt2@linagora.com")
      .state("transport")
      .build()

    val mail2: Mail = FakeMail.builder()
      .name("mail2")
      .sender("sender2@domain.tld")
      .recipients("rcpt1@linagora.com", "rcpt3@linagora.com", "rcpt4@linagora.com")
      .state("transport")
      .build()

    val mail3: Mail = FakeMail.builder()
      .name("mail3")
      .sender("sender3@domain.tld")
      .recipients("rcpt5@linagora.com")
      .state("transport")
      .build()

    mailet.service(mail1)
    mailet.service(mail2)
    mailet.service(mail3)

    SoftAssertions.assertSoftly(softly => {
      softly.assertThat(mail1.getState).isEqualTo("transport")
      softly.assertThat(mail2.getState).isEqualTo("error")
      softly.assertThat(mail3.getState).isEqualTo("transport")
    })
  }

  @Test
  def shouldRateLimitSizeOfEmails(): Unit = {
    val mailet = testee(FakeMailetConfig.builder()
      .mailetName("GlobalRateLimit")
      .setProperty("duration", "20s")
      .setProperty("size", "100K")
      .build())

    val mail1: Mail = FakeMail.builder()
      .name("mail1")
      .sender("sender1@domain.tld")
      .size(50 * 1024)
      .state("transport")
      .build()

    val mail2: Mail = FakeMail.builder()
      .name("mail2")
      .sender("sender2@domain.tld")
      .size(51 * 1024)
      .state("transport")
      .build()

    val mail3: Mail = FakeMail.builder()
      .name("mail3")
      .sender("sender3@domain.tld")
      .size(49 * 1024)
      .state("transport")
      .build()

    mailet.service(mail1)
    mailet.service(mail2)
    mailet.service(mail3)

    SoftAssertions.assertSoftly(softly => {
      softly.assertThat(mail1.getState).isEqualTo("transport")
      softly.assertThat(mail2.getState).isEqualTo("error")
      softly.assertThat(mail3.getState).isEqualTo("transport")
    })
  }

  @Test
  def shouldRateLimitTotalSizeOfEmails(): Unit = {
    val mailet = testee(FakeMailetConfig.builder()
      .mailetName("GlobalRateLimit")
      .setProperty("duration", "20s")
      .setProperty("totalSize", "1M")
      .build())

    val mail1: Mail = FakeMail.builder()
      .name("mail1")
      .sender("sender1@domain.tld")
      .recipients("rcpt1@linagora.com", "rcpt2@linagora.com")
      .size(50 * 1024)
      .state("transport")
      .build()

    val mail2: Mail = FakeMail.builder()
      .name("mail2")
      .sender("sender2@domain.tld")
      .recipients("rcpt1@linagora.com", "rcpt3@linagora.com", "rcpt4@linagora.com")
      .size(311 * 1024)
      .state("transport")
      .build()

    val mail3: Mail = FakeMail.builder()
      .name("mail3")
      .sender("sender3@domain.tld")
      .recipients("rcpt1@linagora.com", "rcpt3@linagora.com")
      .size(210 * 1024)
      .state("transport")
      .build()

    mailet.service(mail1)
    mailet.service(mail2)
    mailet.service(mail3)

    SoftAssertions.assertSoftly(softly => {
      softly.assertThat(mail1.getState).isEqualTo("transport")
      softly.assertThat(mail2.getState).isEqualTo("error")
      softly.assertThat(mail3.getState).isEqualTo("transport")
    })
  }

  @Test
  def totalSizeShouldRejectWhenOverflowing(): Unit = {
    val mailet = testee(FakeMailetConfig.builder()
      .mailetName("GlobalRateLimit")
      .setProperty("duration", "20s")
      .setProperty("totalSize", "1G")
      .build())

    val recipients = (1 to 450).map(i => new MailAddress(s"rcpt$i@domain.tld")).toList

    // 450 * 10 MB = 4.5GB (overflow to a positive integer)
    val mail: Mail = FakeMail.builder()
      .name("mail1")
      .sender("sender@domain.tld")
      .recipients(recipients.asJava)
      .size(10L * 1024L * 1024L)
      .state("transport")
      .build()

    mailet.service(mail)

    assertThat(mail.getState).isEqualTo("error")
  }

  @Test
  def overflowOnTotalSizeShouldNotAffectOtherCriteriaWhenUnspecified(): Unit = {
    val mailet = testee(FakeMailetConfig.builder()
      .mailetName("GlobalRateLimit")
      .setProperty("duration", "20s")
      .setProperty("count", "2")
      .build())

    val recipients = (1 to 450).map(i => new MailAddress(s"rcpt$i@domain.tld")).toList

    // 450 * 10 MB = 4.5GB (overflow to a positive integer)
    val mail: Mail = FakeMail.builder()
      .name("mail1")
      .sender("sender@domain.tld")
      .recipients(recipients.asJava)
      .size(10L * 1024L * 1024L)
      .state("transport")
      .build()

    mailet.service(mail)

    assertThat(mail.getState).isEqualTo("transport")
  }

  @Test
  def severalLimitsShouldBeApplied(): Unit = {
    val mailet = testee(FakeMailetConfig.builder()
      .mailetName("GlobalRateLimit")
      .setProperty("duration", "1000s")
      .setProperty("size", "100K")
      .setProperty("totalSize", "150K")
      .setProperty("recipients", "3")
      .build())

    val mail1: Mail = FakeMail.builder()
      .name("mail1")
      .sender("sender1@domain.tld")
      .recipients("rcpt1@linagora.com", "rcpt2@linagora.com")
      .size(50 * 1024)
      .state("transport")
      .build()

    // Will exceed the size and totalSize rate limit
    val mail2: Mail = FakeMail.builder()
      .name("mail2")
      .sender("sender2@domain.tld")
      .recipients("rcpt1@linagora.com")
      .size(60 * 1024)
      .state("transport")
      .build()

    // Will exceed the recipient rate limit
    val mail3: Mail = FakeMail.builder()
      .name("mail3")
      .sender("sender3@domain.tld")
      .recipients("rcpt1@linagora.com", "rcpt3@linagora.com")
      .size(10 * 1024)
      .state("transport")
      .build()

    mailet.service(mail1)
    mailet.service(mail2)
    mailet.service(mail3)

    SoftAssertions.assertSoftly(softly => {
      softly.assertThat(mail1.getState).isEqualTo("transport")
      softly.assertThat(mail2.getState).isEqualTo("error")
      softly.assertThat(mail3.getState).isEqualTo("error")
    })
  }
}
