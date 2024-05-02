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

package org.apache.james.blob.objectstorage.aws;

import org.apache.james.metrics.api.Metric;
import org.apache.james.metrics.api.MetricFactory;

import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;

public class JamesS3MetricPublisher implements MetricPublisher {
    private final Metric maxConcurrency;
    private final Metric availableConcurrency;
    private final Metric leasedConcurrency;
    private final Metric pendingConcurrencyAcquires;

    public JamesS3MetricPublisher(MetricFactory metricFactory) {
        this.maxConcurrency = metricFactory.generate("s3_maxConcurrency");
        this.availableConcurrency = metricFactory.generate("s3_availableConcurrency");
        this.leasedConcurrency = metricFactory.generate("s3_leasedConcurrency");
        this.pendingConcurrencyAcquires = metricFactory.generate("s3_pendingConcurrencyAcquires");
    }

    @Override
    public void publish(MetricCollection s3ClientMetrics) {
        // TODO extract the needed metrics from S3 client metrics (https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/http/HttpMetric.html) and copy them into James metrics
    }

    @Override
    public void close() {

    }
}
