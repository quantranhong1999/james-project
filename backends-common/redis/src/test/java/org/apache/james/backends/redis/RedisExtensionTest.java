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

package org.apache.james.backends.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.Consumer;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.sync.RedisCommands;

class RedisExtensionTest {

    @RegisterExtension
    static RedisExtension redisExtension = new RedisExtension();

    @Test
    void redisExtensionShouldWork(DockerRedis redis) {
        RedisCommands<String, String> client = redis.createClient();
        String key = "KEY1";
        String keyValue = "Value1";
        client.set(key, keyValue);

        assertThat(client.get(key)).isEqualTo(keyValue);
    }

    @Test
    void consumerGroupTest(DockerRedis redis) {
        RedisCommands<String, String> redisCommands = redis.createClient();

        String stream = "weather_sensor:wind";

        // create consumer group
        String consumerGroup = "jamesConsumers";
        redisCommands.xgroupCreate(XReadArgs.StreamOffset.from(stream, "0-0"), consumerGroup,
            XGroupCreateArgs.Builder.mkstream());

        // register 2 consumers to the stream
        publishMessageToRedisStream(redisCommands, stream);
        StreamMessage<String, String> messageByConsumer1 = redisCommands.xreadgroup(
            Consumer.from(consumerGroup, "consumer_1"),
            XReadArgs.StreamOffset.lastConsumed(stream))
            .get(0);
        System.out.println("Consumer is consuming message with id " + messageByConsumer1.getId());

        publishMessageToRedisStream(redisCommands, stream);
        StreamMessage<String, String> messageByConsumer2 = redisCommands.xreadgroup(
                Consumer.from(consumerGroup, "consumer_2"),
                XReadArgs.StreamOffset.lastConsumed(stream))
            .get(0);
        System.out.println("Consumer 2 is consuming message with id " + messageByConsumer2.getId());

        assertThat(messageByConsumer1.getId()).isNotEqualTo(messageByConsumer2.getId())
            .as("Consumer 2 does not consume the same message with Consumer 1");
    }

    @Test
    void ackedMessageTest(DockerRedis redis) {
        RedisCommands<String, String> redisCommands = redis.createClient();

        String stream = "weather_sensor:wind";

        // create consumer group
        String consumerGroup = "jamesConsumers";
        redisCommands.xgroupCreate(XReadArgs.StreamOffset.from(stream, "0-0"), consumerGroup,
            XGroupCreateArgs.Builder.mkstream());

        // GIVEN the consumer 1 does not acked the message after processing it e.g. because of failure
        publishMessageToRedisStream(redisCommands, stream);
        StreamMessage<String, String> messageByConsumer1 = redisCommands.xreadgroup(
                Consumer.from(consumerGroup, "consumer_1"),
                XReadArgs.StreamOffset.from(stream, ">"))
            .get(0);
        System.out.println("Consumer 1 failed to consume message with id " + messageByConsumer1.getId());

        // The consumer 1 can not see the unacked message using offset > (only return new and not unacked messages)
        assertThat(redisCommands.xreadgroup(Consumer.from(consumerGroup, "consumer_1"), XReadArgs.StreamOffset.from(stream, ">")))
            .isEmpty();

        // Other consumers can not see the unacked message even using offset 0 (because the unacked message is only visible to consumer 1 which tried to consume it)
        assertThat(redisCommands.xreadgroup(Consumer.from(consumerGroup, "consumer_2"), XReadArgs.StreamOffset.from(stream, "0")))
            .isEmpty();

        // THEN the consumer 1 can re-processing the unacked message using offset 0
        StreamMessage<String, String> messageReprocessingByConsumer1 = redisCommands.xreadgroup(
                Consumer.from(consumerGroup, "consumer_1"),
                XReadArgs.StreamOffset.from(stream, "0"))
            .get(0);
        assertThat(messageReprocessingByConsumer1.getId()).isEqualTo(messageByConsumer1.getId());
        // Confirm that the message has been processed using XACK
        redisCommands.xack(stream, consumerGroup, messageReprocessingByConsumer1.getId());
        System.out.println("Consumer 1 succeeded to re-consume message with id " + messageReprocessingByConsumer1.getId());

        // There should be no unacked message now
        assertThat(redisCommands.xpending(stream, consumerGroup)
            .getCount())
            .isZero();
    }

    private void publishMessageToRedisStream(RedisCommands<String, String> redisCommands, String redisStream) {
        Map<String, String> messageBody = new HashMap<>();
        messageBody.put( "speed", "15" );
        messageBody.put( "direction", "270" );
        messageBody.put( "sensor_ts", String.valueOf(System.currentTimeMillis()));
        String messageId = redisCommands.xadd(redisStream, messageBody);
        System.out.println(String.format("Message with id %s : %s published to Redis Streams", messageId, messageBody));
    }

}
