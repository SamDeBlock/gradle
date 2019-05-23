/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.publish.maven.internal.publisher

import org.apache.http.conn.HttpHostConnectException
import org.gradle.api.UncheckedIOException
import org.gradle.internal.resource.transport.http.HttpErrorStatusCodeException
import org.gradle.testing.internal.util.Specification
import spock.lang.Unroll

@Unroll
class NetworkOperationBackOffAndRetryTest extends Specification {

    def 'retries operation if transient network issue - #ex'() {
        when:
        int attempts = 0
        Runnable operation = {
            attempts++
            throw ex
        }
        NetworkOperationBackOffAndRetry executer = new NetworkOperationBackOffAndRetry(3, 1)
        executer.withBackoffAndRetry(operation)

        then:
        attempts == 3
        Exception failure = thrown()
        if (failure instanceof UncheckedIOException) {
            assert failure.cause == ex
        } else {
            assert failure == ex
        }

        where:
        ex << [
            new SocketTimeoutException("something went wrong"),
            new HttpHostConnectException(new IOException("something went wrong"), null, null),
            new HttpErrorStatusCodeException("something", "something", 503, "something"),
            new RuntimeException("with cause", new SocketTimeoutException("something went wrong"))
        ]
    }

    def 'does not retry operation if not transient network issue - #ex'() {
        when:
        int attempts = 0
        Runnable operation = {
            attempts++
            throw ex
        }
        NetworkOperationBackOffAndRetry executer = new NetworkOperationBackOffAndRetry(3, 1)
        executer.withBackoffAndRetry(operation)

        then:
        attempts == 1
        Exception failure = thrown()
        failure == ex

        where:
        ex << [
            new RuntimeException("non network issue"),
            new HttpErrorStatusCodeException("something", "something", 400, "something")
        ]
    }
}
