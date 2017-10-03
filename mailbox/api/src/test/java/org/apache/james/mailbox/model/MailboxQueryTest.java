/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.james.mailbox.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MailboxSession.User;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.model.MailboxQuery.Builder;
import org.junit.Before;
import org.junit.Test;

public class MailboxQueryTest {
    private static final String CURRENT_USER = "user";

    private MailboxPath mailboxPath;
    private MailboxSession mailboxSession;

    @Before
    public void setUp() {
        mailboxPath = new MailboxPath("namespace", "user", "name");
        mailboxSession = new MockMailboxSession("user");
    }

    @Test
    public void IsWildShouldReturnTrueWhenOnlyFreeWildcard() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("*")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isWild();
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void IsWildShouldReturnTrueWhenOnlyLocalWildcard() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("%")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isWild();
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void IsWildShouldReturnTrueWhenFreeWildcardAtBeginning() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("*One")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isWild();
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void IsWildShouldReturnTrueWhenLocalWildcardAtBeginning() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("%One")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isWild();
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void IsWildShouldReturnTrueWhenFreeWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("A*A")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isWild();
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void IsWildShouldReturnTrueWhenLocalWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("A%A")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isWild();
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void IsWildShouldReturnTrueWhenFreeWildcardAtEnd() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("One*")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isWild();
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void IsWildShouldReturnTrueWhenLocalWildcardAtEnd() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("One%")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isWild();
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void IsWildShouldReturnFalseWhenEmptyExpression() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isWild();
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void IsWildShouldReturnFalseWhenNullExpression() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression(null)
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isWild();
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void IsWildShouldReturnFalseWhenNoWildcard() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("ONE")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isWild();
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void getCombinedNameShouldWork() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        String actual = testee.getCombinedName();
        //Then
        assertThat(actual).isEqualTo("name.mailbox");
    }

    @Test
    public void getCombinedNameShouldWorkWhenEmptyExpression() throws Exception { 
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("")
            .mailboxSession(mailboxSession)
            .build();
        //When
        String actual = testee.getCombinedName();
        //Then
        assertThat(actual).isEqualTo("name");
    }

    @Test
    public void getCombinedNameShouldReturnEmptyStringWhenNullMailboxPathAndExpression() throws Exception {
        //Given
        MailboxPath nullMailboxPath = new MailboxPath(null, null, null);
        MailboxQuery testee = MailboxQuery.builder()
            .base(nullMailboxPath)
            .expression(null)
            .mailboxSession(mailboxSession)
            .build();
        //When
        String actual = testee.getCombinedName();
        //Then
        assertThat(actual).isEmpty();
    }

    @Test
    public void getCombinedNameShouldIgnoreDelimiterWhenPresentAtBeginningOfExpression() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression(".mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        String actual = testee.getCombinedName();
        //Then
        assertThat(actual).isEqualTo("name.mailbox");
    }

    @Test
    public void getCombinedNameShouldIgnoreDelimiterWhenPresentAtEndOfMailboxName() throws Exception {
        //Given
        MailboxPath mailboxPathWithNullNamespaceAndUser = new MailboxPath(null, null, "name.");
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPathWithNullNamespaceAndUser)
            .expression("mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        String actual = testee.getCombinedName();
        //Then
        assertThat(actual).isEqualTo("name.mailbox");
    }

    @Test
    public void getCombinedNameShouldIgnoreDelimiterWhenPresentAtBeginningOfExpressionAndEndOfMailboxName() throws Exception {
        //Given
        MailboxPath mailboxPathWithNullNamespaceAndUser = new MailboxPath(null, null, "name.");
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPathWithNullNamespaceAndUser)
            .expression(".mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        String actual = testee.getCombinedName();
        //Then
        assertThat(actual).isEqualTo("name.mailbox");
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenNullExpression() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression(null)
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("folder");
        //Then
        assertThat(actual).isFalse();
    }

    @Test 
    public void isExpressionMatchShouldMatchFolderWhenMatching() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenNameBeginsWithDelimiter() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch(".mailbox");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenNameEndsWithDelimiter() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox.");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldNotMatchFolderWhenNoMatching() throws Exception {
        //Given

        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldNotMatchFolderWithExpandedEndName() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox123");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldNotMatchSubFolder() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox.123");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldReturnTrueWhenEmptyNameAndExpression() throws Exception {
        //Given

        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenEmptyExpressionAndNameBeginsWithDelimiter() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch(".123");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldNotMatchFolderWhenEmptyExpression() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("folder");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldReturnTrueWhenEmptyNameAndOnlyLocalWildcard() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("%")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldReturnTrueWhenOnlyLocalWildcard() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("%")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("folder");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldNotMatchSubFolderWhenOnlyLocalWildcard() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("%")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox.sub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldReturnTrueWhenEmptyNameAndOnlyFreeWildcard() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("*")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldMatchFolderWhenOnlyFreeWildcard() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("*")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldMatchSubFolderWhenOnlyFreeWildcard() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("*")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox.sub");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenEmptyNameAndLocalWildcardAtEnd() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("mailbox%")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenLocalWildcardAtEndAndNoMatching() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("mailbox%")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchFolderWhenLocalWildcardAtEndNotUsed() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("mailbox%")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldReturnTrueWhenLocalWildcardAtEndUsed() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("mailbox%")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailboxsub");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldNotMatchSubFolderWhenLocalWildcardAtEnd() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("mailbox%")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox.sub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenEmptyNameAndLocalWildcardAtBeginning() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldNotMatchFolderWhenLocalWildcardAtBeginningAndNoMatching() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchFolderWhenLocalWildcardAtBeginningNotUsed() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldMatchFolderWhenLocalWildcardAtBeginningUsed() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("submailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldNotMatchSubFolderWhenLocalWildcardAtBeginning() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub.mailbox");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldNotMatchDeeplyNestedFolderWhenLocalWildcardAtBeginning() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub.mailbox.sub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenEmptyNameAndLocalWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenLocalWildcardInMiddleAndMissingEndName() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchFolderWhenLocalWildcardInMiddleAndMatching() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("submailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldMatchFolderWhenLocalWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub123mailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldNotMatchSubFolderWhenLocalWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub.mailbox");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldNotMatchSubFolderWhenLocalWildcardInMiddleAndExpandedMiddleName() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub.123mailbox");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenLocalWildcardInMiddleAndMissingBeginningName() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldNotMatchDeeplyNestedFolderWhenLocalWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("subw.hat.eve.rmailbox");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchSubFolderWhenFreeWildcardAtEnd() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("mailbox*")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox.sub");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenEmptyNameAndFreeWildcardAtEnd() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("mailbox*")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldNotMatchFolderWhenFreeWildcardAtEndAndNoMatching() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("mailbox*")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchFolderWhenFreeWildcardAtEndNotUsed() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("mailbox*")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldMatchFolderWhenFreeWildcardAtEndUsed() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("mailbox*")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox123");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenEmptyNameAndFreeWildcardAtBeginning() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("*mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldNotMatchFolderWhenFreeWildcardAtBeginningAndNoMatching() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("*mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchFolderWhenFreeWildcardAtBeginningNotUsed() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("*mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldMatchFolderWhenFreeWildcardAtBeginningUsed() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("*mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("submailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldMatchSubFolderWhenFreeWildcardAtBeginning() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("*mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub.mailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenEmptyNameAndFreeWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldNotMatchFolderWhenFreeWildcardInMiddleAndMissingEndName() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchFolderWhenFreeWildcardInMiddleNotUsed() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("submailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldMatchSubFolderWhenFreeWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub.mailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenFreeWildcardInMiddleNotUsedAndMissingBeginningName() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchDeeplyNestedFolderWhenFreeWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("subw.hat.eve.rmailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenEmptyNameAndDoubleFreeWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub**mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenDoubleFreeWildcardInMiddleAndMissingEndName() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub**mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldReturnTrueWhenDoubleFreeWildcardInMiddleNotUsed() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub**mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("submailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldMatchSubFolderWhenDoubleFreeWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub**mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub.mailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenDoubleFreeWildcardInMiddleAndMissingBeginningName() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub**mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchDeeplyNestedFolderWhenDoubleFreeWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub**mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("subw.hat.eve.rmailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenEmptyNameAndFreeLocalWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("");
        //Then
        assertThat(actual).isFalse();
    } 

    @Test
    public void isExpressionMatchShouldReturnFalseWhenFreeLocalWildcardInMiddleAndMissingEndName() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchFolderWhenFreeLocalWildcardInMiddleNotUsed() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("submailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldMatchSubFolderWhenFreeLocalWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub.mailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenFreeLocalWildcardInMiddleAndMissingBeginningName() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchDeeplyNestedFolderWhenFreeLocalWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*%mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("subw.hat.eve.rmailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldReturnFalseWhenEmptyNameAndLocalFreeWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub%*mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("");
        //Then
        assertThat(actual).isFalse();
    } 

    @Test
    public void isExpressionMatchShouldNotMatchFolderWhenLocalFreeWildcardInMiddleAndMissingEndName() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub%*mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchFolderWhenLocalFreewildcardInMiddleNotUsed() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub%*mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("submailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldMatchSubFolderWhenLocalFreeWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub%*mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub.mailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldNotMatchFolderWhenLocalFreeWildcardInMiddleAndMissingBeginningName() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub%*mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchDeeplyNestedFolderWhenLocalFreeWildcardInMiddle() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub%*mailbox")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("subw.hat.eve.rmailbox");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldMatchFolderWhenMultipleFreeWildcards() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox*sub**")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("submailboxsub");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldMatchDeeplyNestedFolderWhenMultipleFreeWildcardsNotUsed() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox*sub**")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub.mailbox.sub");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldMatchDeeplyNestedFolderWhenMultipleFreeWildcardsUsed() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox*sub**")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("subtosh.boshmailboxtosh.boshsubboshtosh");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldNotMatchDeeplyNestedFolderWhenMultipleFreeWildcardsAndMissingMiddleName() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox*sub**")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub.a.sub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldNotMatchDeeplyNestedFolderWhenMultipleFreeWildcardsAndMissingEndName() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox*sub**")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub.a.submailbox.u");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldNotMatchDeeplyNestedFolderWhenMultipleFreeWildcardsAndMissingBeginningdName() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox*sub**")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("utosh.boshmailboxtosh.boshsubasubboshtoshmailboxu");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchFolderWhenMixedLocalFreeWildcardsNotUsed() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub%mailbox*sub")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("submailboxsub");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldNotMatchSubFolderWhenMixedLocalFreeWildcards() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub%mailbox*sub")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub.mailboxsub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchFolderWhenMixedFreeLocalWildcardsNotUsed() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox%sub")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("submailboxsub");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldMatchSubFolderWhenMixedFreeLocalWildcards() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox%sub")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub.mailboxsub");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldNotMatchSubFolderWhenMixedFreeLocalWildcards() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox%sub")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("submailbox.sub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchFolderWhenMixedFreeLocalWildcards() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox%sub")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("submailboxwhateversub");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldNotMatchSubFolderEndingWithDelimiterWhenMixedFreeLocalWildcards() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox%sub")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("submailboxsub.Whatever.");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldNotMatchDeeplyNestedFolderWhenMixedFreeLocalWildcards() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox%sub")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub.mailboxsub.sub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchSubFoldeWhenMixedFreeLocalWildcards() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox%sub")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub.mailboxsub");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldMatchDeeplyNestedFoldeWhenMixedFreeLocalWildcards() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("sub*mailbox%sub")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("sub.whatever.mailbox123sub");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldNotMatchFolderWhenTwoLocalPathDelimitedWildcards() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("%.%")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldNotMatchDeeplyNestedFolderWhenTwoLocalPathDelimitedWildcards() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("%.%")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox.sub.sub");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldMatchSubFolderWhenTwoLocalPathDelimitedWildcards() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("%.%")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("mailbox.sub");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldMatchSubFolderWhenFreeWildcardAndPathDelimiterAtBeginning() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("*.test")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("blah.test");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldNotMatchSubFolderWhenWhenFreeWildcardAndPathDelimiterAtBeginning() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("*.test")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("blah.test3");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldNotMatchDeeplyNestedFolderWhenFreeWildcardAndPathDelimiterAtBeginning() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("*.test")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("blah.test.go");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldIgnoreRegexInjection() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("folder^$!)(%3")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("folder^$!)(123");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldIgnoreRegexInjectionWhenUsingEndOfQuoteAndNoMatching() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("\\Efo.")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("\\Efol");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldIgnoreRegexInjectionWhenUsingEndOfQuoteAndMatching() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("\\Efo.")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("\\Efo.");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldIgnoreRegexInjectionWhenUsingBeginOfQuoteAndNoMatching() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("\\Qfo?")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("\\Qfol");
        //Then
        assertThat(actual).isFalse();
    }

    @Test
    public void isExpressionMatchShouldIgnoreRegexInjectionWhenUsingBeginOfQuoteAndMatching() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("\\Qfo?")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("\\Qfo?");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldNotEscapeFreeWildcard() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("folder\\*")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("folder\\123");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void isExpressionMatchShouldNotEscapeLocalWildcard() throws Exception {
        //Given
        MailboxQuery testee = MailboxQuery.builder()
            .base(mailboxPath)
            .expression("folder\\%")
            .mailboxSession(mailboxSession)
            .build();
        //When
        boolean actual = testee.isExpressionMatch("folder\\123");
        //Then
        assertThat(actual).isTrue();
    }

    @Test
    public void buildShouldMatchAllValuesWhenMatchesAll() throws Exception {
        //When
        MailboxQuery actual = MailboxQuery.builder()
                .base(mailboxPath)
                .matchesAll()
                .mailboxSession(mailboxSession)
                .build();
        //Then
        assertThat(actual.isExpressionMatch("folder")).isTrue();
    }

    @Test
    public void buildShouldConstructMailboxPathWhenPrivateUserMailboxes() throws Exception {
        //Given
        MailboxPath expected = MailboxPath.forUser("user", "");
        //When
        MailboxQuery actual = MailboxQuery.builder()
                .username("user")
                .privateMailboxes()
                .mailboxSession(mailboxSession)
                .build();
        //Then
        assertThat(actual.getBase()).isEqualTo(expected);
    }

    @Test
    public void buildShouldMatchAllValuesWhenPrivateUserMailboxes() throws Exception {
        //Given
        Builder testee = MailboxQuery.builder()
                .username("user")
                .privateMailboxes()
            .mailboxSession(mailboxSession);
        //When
        MailboxQuery actual = testee.build();
        //Then
        assertThat(actual.isExpressionMatch("folder")).isTrue();
    }

    @Test
    public void builderShouldInitFromSessionWhenGiven() throws Exception {
        //Given
        MailboxSession mailboxSession = mock(MailboxSession.class);
        when(mailboxSession.getPathDelimiter()).thenReturn('#');
        User user = mock(User.class);
        when(user.getUserName()).thenReturn("little bobby table");
        when(mailboxSession.getUser()).thenReturn(user);
        // When
        Builder query = MailboxQuery.privateMailboxesBuilder(mailboxSession);
        //Then
        assertThat(query.mailboxSession.getPathDelimiter()).isEqualTo('#');
        assertThat(query.username).contains("little bobby table");
    }

    @Test
    public void builderShouldNotThrowWhenNoBaseDefined() throws Exception {
        //Given
        Builder testee = MailboxQuery.builder()
                .expression("abc")
                .mailboxSession(mailboxSession);
        //When
        testee.build();
    }

    @Test
    public void getPathLikeShouldReturnUserPathLikeWhenNoBaseDefined() throws Exception {
        //Given
        Builder testee = MailboxQuery.builder()
            .expression("abc")
            .mailboxSession(mailboxSession);
        //When
        MailboxQuery mailboxQuery = testee.build();

        assertThat(mailboxQuery.getPathLike()).isEqualTo(MailboxPath.forUser("user", "abc%"));
    }

    @Test(expected=IllegalStateException.class)
    public void builderShouldThrowWhenBaseAndUsernameGiven() throws Exception {
        //Given
        Builder testee = MailboxQuery.builder()
                .base(mailboxPath)
                .username("user");
        //When
        testee.build();
    }

    @Test(expected=IllegalStateException.class)
    public void builderShouldThrowWhenBaseGiven() throws Exception {
        //Given
        Builder testee = MailboxQuery.builder()
                .base(mailboxPath)
                .privateMailboxes();
        //When
        testee.build();
    } 

    @Test(expected=IllegalStateException.class)
    public void builderShouldThrowWhenMissingUsername() throws Exception {
        //Given
        Builder testee = MailboxQuery.builder()
                .privateMailboxes();
        //When
        testee.build();
    }

    @Test
    public void builderShouldUseBaseWhenGiven() throws Exception {
        //When
        MailboxQuery actual = MailboxQuery.builder()
                .base(mailboxPath)
                .mailboxSession(mailboxSession)
                .build();
        //Then
        assertThat(actual.getBase()).isEqualTo(mailboxPath);
    }

    @Test
    public void belongsToNamespaceAndUserShouldReturnTrueWithIdenticalMailboxes() {
        MailboxQuery mailboxQuery = MailboxQuery.builder()
            .base(mailboxPath)
            .mailboxSession(mailboxSession)
            .build();

        assertThat(mailboxQuery.belongsToRequestedNamespaceAndUser(mailboxPath))
            .isTrue();
    }

    @Test
    public void belongsToNamespaceAndUserShouldReturnTrueWithIdenticalMailboxesWithNullUser() {
        MailboxPath base = new MailboxPath("namespace", null, "name");

        MailboxQuery mailboxQuery = MailboxQuery.builder()
            .base(base)
            .mailboxSession(mailboxSession)
            .build();

        assertThat(mailboxQuery.belongsToRequestedNamespaceAndUser(mailboxPath))
            .isTrue();
    }

    @Test
    public void belongsToNamespaceAndUserShouldReturnTrueWithIdenticalMailboxesWithNullNamespace() {
        MailboxPath mailboxPath = new MailboxPath(null, "user", "name");

        MailboxQuery mailboxQuery = MailboxQuery.builder()
            .base(mailboxPath)
            .mailboxSession(mailboxSession)
            .build();

        assertThat(mailboxQuery.belongsToRequestedNamespaceAndUser(mailboxPath))
            .isTrue();
    }

    @Test
    public void belongsToNamespaceAndUserShouldReturnTrueWithMailboxWithSameNamespaceAndUserWithNullUser() {
        MailboxQuery mailboxQuery = MailboxQuery.builder()
            .base(new MailboxPath("namespace", null, "name"))
            .mailboxSession(mailboxSession)
            .build();

        assertThat(mailboxQuery.belongsToRequestedNamespaceAndUser(new MailboxPath("namespace", null, "name")))
            .isTrue();
    }

    @Test
    public void belongsToNamespaceAndUserShouldReturnTrueWithMailboxWithSameNamespaceAndUser() {
        MailboxQuery mailboxQuery = MailboxQuery.builder()
            .base(new MailboxPath("namespace", CURRENT_USER, "name"))
            .mailboxSession(mailboxSession)
            .build();

        assertThat(mailboxQuery.belongsToRequestedNamespaceAndUser(new MailboxPath("namespace", CURRENT_USER, "name2")))
            .isTrue();
    }

    @Test
    public void belongsToNamespaceAndUserShouldReturnFalseWithDifferentNamespace() {
        MailboxQuery mailboxQuery = MailboxQuery.builder()
            .base(new MailboxPath("namespace", CURRENT_USER, "name"))
            .mailboxSession(mailboxSession)
            .build();

        assertThat(mailboxQuery.belongsToRequestedNamespaceAndUser(new MailboxPath("namespace2", CURRENT_USER, "name")))
            .isFalse();
    }

    @Test
    public void belongsToNamespaceAndUserShouldReturnFalseWithDifferentUser() {
        MailboxQuery mailboxQuery = MailboxQuery.builder()
            .base(new MailboxPath("namespace", CURRENT_USER, "name"))
            .mailboxSession(mailboxSession)
            .build();

        assertThat(mailboxQuery.belongsToRequestedNamespaceAndUser(new MailboxPath("namespace", CURRENT_USER + "2", "name")))
            .isFalse();
    }
    @Test
    public void belongsToNamespaceAndUserShouldReturnFalseWithOneOfTheUserNull() {
        MailboxQuery mailboxQuery = MailboxQuery.builder()
            .base(new MailboxPath("namespace", CURRENT_USER, "name"))
            .mailboxSession(mailboxSession)
            .build();

        assertThat(mailboxQuery.belongsToRequestedNamespaceAndUser(new MailboxPath("namespace", null + "2", "name")))
            .isFalse();
    }
    @Test
    public void belongsToNamespaceAndUserShouldReturnFalseIfNamespaceAreDifferentWithNullUser() {
        MailboxQuery mailboxQuery = MailboxQuery.builder()
            .base(new MailboxPath("namespace", null, "name"))
            .mailboxSession(mailboxSession)
            .build();

        assertThat(mailboxQuery.belongsToRequestedNamespaceAndUser(new MailboxPath("namespace2", null, "name")))
            .isFalse();
    }
}
