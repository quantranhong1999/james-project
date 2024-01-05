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

package org.apache.james;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.james.data.UsersRepositoryModuleChooser.Implementation.DEFAULT;
import static org.apache.james.model.CrowdsecClientConfiguration.DEFAULT_API_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;

import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.net.smtp.SMTPClient;
import org.apache.james.model.CrowdsecClientConfiguration;
import org.apache.james.model.CrowdsecDecision;
import org.apache.james.model.CrowdsecHttpClient;
import org.apache.james.modules.protocols.ImapGuiceProbe;
import org.apache.james.modules.protocols.SmtpGuiceProbe;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.TestIMAPClient;
import org.assertj.core.api.SoftAssertions;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.utility.MountableFile;

import com.github.fge.lambdas.Throwing;

class CrowdsecIntegrationTest {
    @RegisterExtension
    static CrowdsecExtension crowdsecExtension = new CrowdsecExtension();

    @RegisterExtension
    static JamesServerExtension testExtension = new JamesServerBuilder<MemoryJamesConfiguration>(tmpDir ->
        MemoryJamesConfiguration.builder()
            .workingDirectory(tmpDir)
            .configurationFromClasspath()
            .usersRepository(DEFAULT)
            .build())
        .server(MemoryJamesServerMain::createServer)
        .extension(crowdsecExtension)
        .lifeCycle(JamesServerExtension.Lifecycle.PER_TEST)
        .build();

    @RegisterExtension
    public TestIMAPClient testIMAPClient = new TestIMAPClient();

    private static final String LOCALHOST_IP = "127.0.0.1";
    private static final String DOMAIN = "domain.tld";
    private static final String BOB = "bob@" + DOMAIN;
    private static final String BOB_PASSWORD = "bobPassword";
    private static final String BAD_PASSWORD = "badPassword";
    private static final ConditionFactory CALMLY_AWAIT = Awaitility
        .with().pollInterval(ONE_HUNDRED_MILLISECONDS)
        .and().pollDelay(ONE_HUNDRED_MILLISECONDS)
        .await();
    private static final String CLIENT_IP = "255.255.255.254";
    private static final String PROXY_IP = "255.255.255.255";

    private SocketChannel clientConnection;

    private GenericContainer<?> haproxyContainer;

    private HAProxyExtension haProxyExtension;
    private SMTPClient smtpProtocol;
    private CrowdsecHttpClient crowdsecClient;

    @BeforeEach
    void setup(GuiceJamesServer server, @TempDir Path tempDir) throws Exception {
        server.getProbe(DataProbeImpl.class).fluent()
            .addDomain(DOMAIN)
            .addUser(BOB, BOB_PASSWORD);

        haProxyExtension = new HAProxyExtension(MountableFile.forHostPath(createHaProxyConfigFile(server, tempDir).toString()));
        haProxyExtension.start();

        smtpProtocol = new SMTPClient();
        crowdsecClient = new CrowdsecHttpClient(new CrowdsecClientConfiguration(crowdsecExtension.getCrowdSecUrl(), DEFAULT_API_KEY));
    }

    private Path createHaProxyConfigFile(GuiceJamesServer server, Path tempDir) throws IOException {
        String smtpServerWithProxySupportIp = crowdsecExtension.getCrowdsecContainer().getContainerInfo().getNetworkSettings()
            .getGateway(); // James server listens at the docker bridge network's gateway IP
        int smtpWithProxySupportPort = server.getProbe(SmtpGuiceProbe.class).getSmtpAuthRequiredPort().getValue();
        String haproxyConfigContent = String.format("global\n" +
                "  log stdout format raw local0 info\n" +
                "\n" +
                "defaults\n" +
                "  mode tcp\n" +
                "  timeout client 1800s\n" +
                "  timeout connect 5s\n" +
                "  timeout server 1800s\n" +
                "  log global\n" +
                "  option tcplog\n" +
                "\n" +
                "frontend smtp-frontend\n" +
                "  bind :25\n" +
                "  default_backend james-server-smtp\n" +
                "\n" +
                "backend james-server-smtp\n" +
                "  server james1 %s:%d send-proxy\n",
            smtpServerWithProxySupportIp, smtpWithProxySupportPort);
        Path haProxyConfigFile = tempDir.resolve("haproxy.cfg");
        Files.write(haProxyConfigFile, haproxyConfigContent.getBytes());

        return haProxyConfigFile;
    }

    @AfterEach
    void teardown() {
        haProxyExtension.stop();
    }

    @Nested
    class IMAP {
        @Test
        void ipShouldBeBannedByCrowdSecWhenFailingToImapLoginThreeTimes(GuiceJamesServer server) {
            // GIVEN an IP failed to log in 3 consecutive times in a short period
            IntStream.range(0, 3)
                .forEach(any ->
                    assertThatThrownBy(() -> testIMAPClient.connect("127.0.0.1", server.getProbe(ImapGuiceProbe.class).getImapPort())
                        .login(BOB, BAD_PASSWORD))
                        .isInstanceOf(IOException.class)
                        .hasMessage("Login failed"));

        // THEN connection from the IP would be blocked. CrowdSec takes time to processing the ban decision therefore the await below.
        CALMLY_AWAIT.atMost(Durations.TEN_SECONDS)
            .untilAsserted(() -> assertThatThrownBy(() -> testIMAPClient.connect("127.0.0.1", server.getProbe(ImapGuiceProbe.class).getImapPort()))
                .isInstanceOf(EOFException.class)
                .hasMessage("Connection closed without indication."));
    }

        @Test
        void ipConnectViaProxyShouldBeBannedByCrowdSecWhenFailingToImapLoginThreeTimes(GuiceJamesServer server) throws IOException, InterruptedException {
            // GIVEN an IP failed to log in 3 consecutive times in a short period
            String CLIENT_IP = "127.0.0.1";
            clientConnection = SocketChannel.open();

//        clientConnection.connect(new InetSocketAddress(haProxyExtension.getHaproxyContainer().getHost(), haProxyExtension.getHaproxyContainer().getMappedPort(IMAPS_PORT)));
            readBytes(clientConnection);
            IntStream.range(0, 3)
                .forEach(any -> {
                    try {
                        clientConnection.write(ByteBuffer.wrap(String.format("a0 LOGIN %s %s\r\n",
                            BOB, BAD_PASSWORD).getBytes(StandardCharsets.UTF_8)));
                        assertThat(new String(readBytes(clientConnection), StandardCharsets.UTF_8))
                            .doesNotStartWith("a0 OK");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }});
            clientConnection.finishConnect();
            // THEN connection from the IP would be blocked. CrowdSec takes time to processing the ban decision therefore the await below.
//        CALMLY_AWAIT.atMost(Durations.TEN_SECONDS)
//            .untilAsserted(() -> assertThatThrownBy(() ->  clientConnection.connect(new InetSocketAddress(haProxyExtension.getHaproxyContainer().getHost(), haProxyExtension.getHaproxyContainer().getMappedPort(IMAPS_PORT))))
//                .isInstanceOf(EOFException.class)
//                .hasMessage("Connection closed without indication.");
        }

        private String[] createCommandHAProxyConfig(GuiceJamesServer server) {
            int port = server.getProbe(ImapGuiceProbe.class).getImapSSLPort();
            String haproxyConfig =
                "global\n" +
                    "  log stdout format raw local0 info\n" +
                    "\n" +
                    "defaults\n" +
                    "  mode tcp\n" +
                    "  timeout client 1800s\n" +
                    "  timeout connect 5s\n" +
                    "  timeout server 1800s\n" +
                    "  log global\n" +
                    "  option tcplog\n" +
                    "frontend imaps-frontend\n" +
                    "  bind :" + port + "\n" +
                    "  default_backend james-servers-imaps\n" +
                    "\n" +
                    "backend james-servers-imaps\n" +
                    "  server james1 localhost:" + port + " send-proxy";
            return new String[]{"/bin/sh", "-c", "haproxy -f <(echo \"" + haproxyConfig + "\")"};
        }

        private void createFile(GuiceJamesServer server) throws IOException {
            int imapPort = server.getProbe(ImapGuiceProbe.class).getImapPort();
            int imapPortSSL = server.getProbe(ImapGuiceProbe.class).getImapSSLPort();

            String haproxyConfig =
                "global\n" +
                    "  log stdout format raw local0 info\n" +
                    "\n" +
                    "defaults\n" +
                    "  mode tcp\n" +
                    "  timeout client 1800s\n" +
                    "  timeout connect 5s\n" +
                    "  timeout server 1800s\n" +
                    "  log global\n" +
                    "  option tcplog\n" +
                    "frontend imap1-frontend\n" +
                    "  bind :" + 143 + "\n" +
                    "  default_backend james-servers-imaps\n" +
                    "frontend imaps-frontend\n" +
                    "  bind :" + 993 + "\n" +
                    "  default_backend james-servers-imaps\n" +
                    "\n" +
                    "backend james-servers-imap\n" +
                    "  server james1 172.17.0.1:" + imapPort + " send-proxy\n" +
                    "backend james-servers-imaps\n" +
                    "  server james1 172.17.0.1:" + imapPortSSL + " send-proxy";

            File file = new File("/home/linagora/apache-james/james-project/third-party/crowdsec/src/test/resources/" + File.separator + "haproxy.cfg");
            if (file.exists()) {
                file.delete();
            }

            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.write(haproxyConfig);
            writer.close();
        }

        private byte[] readBytes(SocketChannel channel) throws IOException {
            ByteBuffer line = ByteBuffer.allocate(1024);
            channel.read(line);
            line.rewind();
            byte[] bline = new byte[line.remaining()];
            line.get(bline);
            return bline;
        }
}

    @Nested
    class SMTP {
        @Test
        void ehloShouldRejectAfterThreeFailedSMTPAuthentications(GuiceJamesServer server) {
            // GIVEN a client failed to log in 3 consecutive times in a short period
            IntStream.range(0, 3)
                .forEach(Throwing.intConsumer(any -> {
                    smtpProtocol.connect(LOCALHOST_IP, server.getProbe(SmtpGuiceProbe.class).getSmtpPort().getValue());
                    smtpProtocol.sendCommand("EHLO", InetAddress.getLocalHost().toString());
                    smtpProtocol.sendCommand("AUTH PLAIN");
                    smtpProtocol.sendCommand(Base64.getEncoder().encodeToString(("\0" + BOB + "\0" + BAD_PASSWORD + "\0").getBytes(UTF_8)));
                    assertThat(smtpProtocol.getReplyString())
                        .contains("535 Authentication Failed");
                }));

            // THEN SMTP usage from the IP would be blocked upon the EHLO command. CrowdSec takes time to processing the ban decision therefore the await below.
            CALMLY_AWAIT.atMost(Durations.TEN_SECONDS)
                .untilAsserted(() -> {
                    smtpProtocol.connect(LOCALHOST_IP, server.getProbe(SmtpGuiceProbe.class).getSmtpPort().getValue());
                    smtpProtocol.sendCommand("EHLO", InetAddress.getLocalHost().toString());
                    assertThat(smtpProtocol.getReplyString())
                        .contains("554 Email rejected");
                });
        }

        @Test
        void ehloShouldRejectAfterThreeFailedSMTPAuthenticationsViaProxy() {
            // GIVEN a client connected via proxy failed to log in 3 consecutive times in a short period
            IntStream.range(0, 3)
                .forEach(Throwing.intConsumer(any -> {
                    smtpProtocol.connect(LOCALHOST_IP, haProxyExtension.getProxiedSmtpPort());
                    smtpProtocol.sendCommand("EHLO", InetAddress.getLocalHost().toString());
                    smtpProtocol.sendCommand("AUTH PLAIN");
                    smtpProtocol.sendCommand(Base64.getEncoder().encodeToString(("\0" + BOB + "\0" + BAD_PASSWORD + "\0").getBytes(UTF_8)));
                    assertThat(smtpProtocol.getReplyString())
                        .contains("535 Authentication Failed");
                }));

            // THEN SMTP usage from the client would be blocked upon the EHLO command.
            CALMLY_AWAIT.atMost(Durations.TEN_SECONDS)
                .untilAsserted(() -> {
                    smtpProtocol.connect(LOCALHOST_IP, haProxyExtension.getProxiedSmtpPort());
                    smtpProtocol.sendCommand("EHLO", InetAddress.getLocalHost().toString());
                    assertThat(smtpProtocol.getReplyString())
                        .contains("554 Email rejected");
                });
        }

        @Test
        void shouldBanRealClientIpAndNotProxyIp() {
            // GIVEN a client connected via proxy failed to log in 3 consecutive times in a short period
            IntStream.range(0, 3)
                .forEach(Throwing.intConsumer(any -> {
                    smtpProtocol.connect(LOCALHOST_IP, haProxyExtension.getProxiedSmtpPort());
                    smtpProtocol.sendCommand("EHLO", InetAddress.getLocalHost().toString());
                    smtpProtocol.sendCommand("AUTH PLAIN");
                    smtpProtocol.sendCommand(Base64.getEncoder().encodeToString(("\0" + BOB + "\0" + BAD_PASSWORD + "\0").getBytes(UTF_8)));
                    assertThat(smtpProtocol.getReplyString())
                        .contains("535 Authentication Failed");
                }));

            CALMLY_AWAIT.atMost(Durations.TEN_SECONDS)
                .untilAsserted(() -> {
                    smtpProtocol.connect(LOCALHOST_IP, haProxyExtension.getProxiedSmtpPort());
                    smtpProtocol.sendCommand("EHLO", InetAddress.getLocalHost().toString());
                    assertThat(smtpProtocol.getReplyString())
                        .contains("554 Email rejected");
                });

            // THEN real client IP must be banned, not proxy IP
            String realClientIp = haProxyExtension.getHaproxyContainer().getContainerInfo().getNetworkSettings()
                .getGateway(); // client connect to HAProxy container via the docker bridge network's gateway IP
            String haProxyIp = haProxyExtension.getHaproxyContainer().getContainerInfo().getNetworkSettings()
                .getIpAddress();

            List<CrowdsecDecision> decisions = crowdsecClient.getCrowdsecDecisions().block();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(decisions).hasSize(1);
                softly.assertThat(decisions.get(0).getValue()).isEqualTo(realClientIp);
                softly.assertThat(decisions.get(0).getValue()).isNotEqualTo(haProxyIp);
            });
        }

    }
}
