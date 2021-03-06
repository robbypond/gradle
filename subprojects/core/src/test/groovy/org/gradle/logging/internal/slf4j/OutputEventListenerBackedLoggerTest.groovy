/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.logging.internal.slf4j

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logging
import org.gradle.logging.internal.LogEvent
import org.gradle.logging.internal.OutputEventListener
import org.slf4j.Marker
import spock.lang.Specification
import spock.lang.Unroll

import static org.gradle.api.logging.LogLevel.*
import static org.slf4j.Logger.ROOT_LOGGER_NAME

@Unroll
class OutputEventListenerBackedLoggerTest extends Specification {

    List<LogEvent> events = []
    OutputEventListenerBackedLoggerContext context

    def setup() {
        context = new OutputEventListenerBackedLoggerContext(System.out, System.err)
        context.outputEventListener = Mock(OutputEventListener) {
            onOutput(_) >> { LogEvent event -> events << event }
        }
    }

    def cleanup() {
        assert !events
    }

    private SingleLogEventSpecificationBuilder singleLogEvent() {
        new SingleLogEventSpecificationBuilder()
    }

    private class SingleLogEventSpecificationBuilder {
        private String category = ROOT_LOGGER_NAME
        private String message
        private long timestamp
        private Throwable throwable
        private LogLevel logLevel

        SingleLogEventSpecificationBuilder message(String message) {
            this.message = message
            this
        }

        SingleLogEventSpecificationBuilder timestamp(long timestamp) {
            this.timestamp = timestamp
            this
        }

        SingleLogEventSpecificationBuilder throwable(Throwable throwable) {
            this.throwable = throwable
            this
        }

        SingleLogEventSpecificationBuilder logLevel(LogLevel logLevel) {
            this.logLevel = logLevel
            this
        }

        void verify(boolean eventExpected) {
            if (!eventExpected) {
                assert events.size() == 0
                return
            }

            assert events.size() == 1
            LogEvent event = events.remove(0)
            assert event.category == category
            assert event.message == message
            assert event.timestamp >= timestamp
            assert event.throwable == throwable
            assert event.logLevel == logLevel
        }
    }

    private OutputEventListenerBackedLogger logger() {
        context.getLogger(ROOT_LOGGER_NAME)
    }

    private void setGlobalLevel(LogLevel level) {
        context.level = level
    }

    def "isTraceEnabled returns false when level is #level"() {
        when:
        globalLevel = level

        then:
        !logger().traceEnabled
        !logger().isTraceEnabled(null)

        where:
        level << LogLevel.values()
    }

    def "isDebugEnabled returns #enabled when level is #level"() {
        when:
        globalLevel = level

        then:
        logger().debugEnabled == enabled
        logger().isDebugEnabled(null) == enabled


        where:
        level     | enabled
        DEBUG     | true
        INFO      | false
        LIFECYCLE | false
        WARN      | false
        QUIET     | false
        ERROR     | false
    }

    def "isInfoEnabled returns #enabled when level is #level"() {
        when:
        globalLevel = level

        then:
        logger().infoEnabled == enabled
        logger().isInfoEnabled(null) == enabled


        where:
        level     | enabled
        DEBUG     | true
        INFO      | true
        LIFECYCLE | false
        WARN      | false
        QUIET     | false
        ERROR     | false
    }

    def "isInfoEnabled with LIFECYCLE marker returns #enabled when level is #level"() {
        when:
        globalLevel = level

        then:
        logger().isInfoEnabled(Logging.LIFECYCLE) == enabled


        where:
        level     | enabled
        DEBUG     | true
        INFO      | true
        LIFECYCLE | true
        WARN      | false
        QUIET     | false
        ERROR     | false
    }

    def "isInfoEnabled with QUIET marker returns #enabled when level is #level"() {
        when:
        globalLevel = level

        then:
        logger().isInfoEnabled(Logging.QUIET) == enabled


        where:
        level     | enabled
        DEBUG     | true
        INFO      | true
        LIFECYCLE | true
        WARN      | true
        QUIET     | true
        ERROR     | false
    }

    def "isWarnEnabled returns #enabled when level is #level"() {
        when:
        globalLevel = level

        then:
        logger().warnEnabled == enabled
        logger().isWarnEnabled(null) == enabled


        where:
        level     | enabled
        DEBUG     | true
        INFO      | true
        LIFECYCLE | true
        WARN      | true
        QUIET     | false
        ERROR     | false
    }

    def "isErrorEnabled returns #enabled when level is #level"() {
        when:
        globalLevel = level

        then:
        logger().errorEnabled == enabled
        logger().isErrorEnabled(null) == enabled


        where:
        level     | enabled
        DEBUG     | true
        INFO      | true
        LIFECYCLE | true
        WARN      | true
        QUIET     | true
        ERROR     | true
    }

    def "trace calls do nothing when level is #level"() {
        given:
        context.outputEventListener = Mock(OutputEventListener)

        and:
        globalLevel = level

        when:
        logger().trace(message)
        logger().trace(message, new Exception())
        logger().trace(message, arg1)
        logger().trace(message, arg1, arg2)
        logger().trace(message, arg1, arg2, arg3)
        logger().trace((Marker) null, message)
        logger().trace((Marker) null, message, new Exception())
        logger().trace((Marker) null, message, arg1)
        logger().trace((Marker) null, message, arg1, arg2)
        logger().trace((Marker) null, message, arg1, arg2, arg3)

        then:
        0 * context.outputEventListener._


        where:
        level << LogLevel.values()

        message = "message"
        arg1 = "arg1"
        arg2 = "arg2"
        arg3 = "arg3"
    }

    def "debug calls work as expected when level is #level"() {
        given:
        globalLevel = level

        when:
        logger().debug("message")

        then:
        singleLogEvent().message("message").logLevel(DEBUG).timestamp(now).verify(eventExpected)

        when:
        logger().debug("{}", arg1)

        then:
        singleLogEvent().message("arg1").logLevel(DEBUG).timestamp(now).verify(eventExpected)

        when:
        logger().debug("{} {}", arg1, arg2)

        then:
        singleLogEvent().message("arg1 arg2").logLevel(DEBUG).timestamp(now).verify(eventExpected)

        when:
        logger().debug("{} {} {}", arg1, arg2, arg3)

        then:
        singleLogEvent().message("arg1 arg2 arg3").logLevel(DEBUG).timestamp(now).verify(eventExpected)

        when:
        logger().debug("message", throwable)

        then:
        singleLogEvent().message("message").logLevel(DEBUG).timestamp(now).throwable(throwable).verify(eventExpected)

        when:
        logger().debug((Marker)null, "message")

        then:
        singleLogEvent().message("message").logLevel(DEBUG).timestamp(now).verify(eventExpected)

        when:
        logger().debug((Marker)null, "{}", arg1)

        then:
        singleLogEvent().message("arg1").logLevel(DEBUG).timestamp(now).verify(eventExpected)

        when:
        logger().debug((Marker)null, "{} {}", arg1, arg2)

        then:
        singleLogEvent().message("arg1 arg2").logLevel(DEBUG).timestamp(now).verify(eventExpected)

        when:
        logger().debug((Marker)null, "{} {} {}", arg1, arg2, arg3)

        then:
        singleLogEvent().message("arg1 arg2 arg3").logLevel(DEBUG).timestamp(now).verify(eventExpected)

        when:
        logger().debug((Marker)null, "message", throwable)

        then:
        singleLogEvent().message("message").logLevel(DEBUG).timestamp(now).throwable(throwable).verify(eventExpected)

        where:
        level     | eventExpected
        DEBUG     | true
        INFO      | false
        LIFECYCLE | false
        WARN      | false
        QUIET     | false
        ERROR     | false

        now = System.currentTimeMillis()
        throwable = new Throwable()
        arg1 = "arg1"
        arg2 = "arg2"
        arg3 = "arg3"
    }

    def "info calls work as expected when level is #level"() {
        given:
        globalLevel = level

        when:
        logger().info("message")

        then:
        singleLogEvent().message("message").logLevel(INFO).timestamp(now).verify(eventExpected)

        when:
        logger().info("{}", arg1)

        then:
        singleLogEvent().message("arg1").logLevel(INFO).timestamp(now).verify(eventExpected)

        when:
        logger().info("{} {}", arg1, arg2)

        then:
        singleLogEvent().message("arg1 arg2").logLevel(INFO).timestamp(now).verify(eventExpected)

        when:
        logger().info("{} {} {}", arg1, arg2, arg3)

        then:
        singleLogEvent().message("arg1 arg2 arg3").logLevel(INFO).timestamp(now).verify(eventExpected)

        when:
        logger().info("message", throwable)

        then:
        singleLogEvent().message("message").logLevel(INFO).timestamp(now).throwable(throwable).verify(eventExpected)

        when:
        logger().info((Marker)null, "message")

        then:
        singleLogEvent().message("message").logLevel(INFO).timestamp(now).verify(eventExpected)

        when:
        logger().info((Marker)null, "{}", arg1)

        then:
        singleLogEvent().message("arg1").logLevel(INFO).timestamp(now).verify(eventExpected)

        when:
        logger().info((Marker)null, "{} {}", arg1, arg2)

        then:
        singleLogEvent().message("arg1 arg2").logLevel(INFO).timestamp(now).verify(eventExpected)

        when:
        logger().info((Marker)null, "{} {} {}", arg1, arg2, arg3)

        then:
        singleLogEvent().message("arg1 arg2 arg3").logLevel(INFO).timestamp(now).verify(eventExpected)

        when:
        logger().info((Marker)null, "message", throwable)

        then:
        singleLogEvent().message("message").logLevel(INFO).timestamp(now).throwable(throwable).verify(eventExpected)

        where:
        level     | eventExpected
        DEBUG     | true
        INFO      | true
        LIFECYCLE | false
        WARN      | false
        QUIET     | false
        ERROR     | false

        now = System.currentTimeMillis()
        throwable = new Throwable()
        arg1 = "arg1"
        arg2 = "arg2"
        arg3 = "arg3"
    }

    def "info calls with LIFECYCLE marker work as expected when level is #level"() {
        given:
        globalLevel = level

        when:
        logger().info(Logging.LIFECYCLE, "message")

        then:
        singleLogEvent().message("message").logLevel(LIFECYCLE).timestamp(now).verify(eventExpected)

        when:
        logger().info(Logging.LIFECYCLE, "{}", arg1)

        then:
        singleLogEvent().message("arg1").logLevel(LIFECYCLE).timestamp(now).verify(eventExpected)

        when:
        logger().info(Logging.LIFECYCLE, "{} {}", arg1, arg2)

        then:
        singleLogEvent().message("arg1 arg2").logLevel(LIFECYCLE).timestamp(now).verify(eventExpected)

        when:
        logger().info(Logging.LIFECYCLE, "{} {} {}", arg1, arg2, arg3)

        then:
        singleLogEvent().message("arg1 arg2 arg3").logLevel(LIFECYCLE).timestamp(now).verify(eventExpected)

        when:
        logger().info(Logging.LIFECYCLE, "message", throwable)

        then:
        singleLogEvent().message("message").logLevel(LIFECYCLE).timestamp(now).throwable(throwable).verify(eventExpected)

        where:
        level     | eventExpected
        DEBUG     | true
        INFO      | true
        LIFECYCLE | true
        WARN      | false
        QUIET     | false
        ERROR     | false

        now = System.currentTimeMillis()
        throwable = new Throwable()
        arg1 = "arg1"
        arg2 = "arg2"
        arg3 = "arg3"
    }

    def "info calls with QUIET marker work as expected when level is #level"() {
        given:
        globalLevel = level

        when:
        logger().info(Logging.QUIET, "message")

        then:
        singleLogEvent().message("message").logLevel(QUIET).timestamp(now).verify(eventExpected)

        when:
        logger().info(Logging.QUIET, "{}", arg1)

        then:
        singleLogEvent().message("arg1").logLevel(QUIET).timestamp(now).verify(eventExpected)

        when:
        logger().info(Logging.QUIET, "{} {}", arg1, arg2)

        then:
        singleLogEvent().message("arg1 arg2").logLevel(QUIET).timestamp(now).verify(eventExpected)

        when:
        logger().info(Logging.QUIET, "{} {} {}", arg1, arg2, arg3)

        then:
        singleLogEvent().message("arg1 arg2 arg3").logLevel(QUIET).timestamp(now).verify(eventExpected)

        when:
        logger().info(Logging.QUIET, "message", throwable)

        then:
        singleLogEvent().message("message").logLevel(QUIET).timestamp(now).throwable(throwable).verify(eventExpected)

        where:
        level     | eventExpected
        DEBUG     | true
        INFO      | true
        LIFECYCLE | true
        WARN      | true
        QUIET     | true
        ERROR     | false

        now = System.currentTimeMillis()
        throwable = new Throwable()
        arg1 = "arg1"
        arg2 = "arg2"
        arg3 = "arg3"
    }

    def "warn calls work as expected when level is #level"() {
        given:
        globalLevel = level

        when:
        logger().warn("message")

        then:
        singleLogEvent().message("message").logLevel(WARN).timestamp(now).verify(eventExpected)

        when:
        logger().warn("{}", arg1)

        then:
        singleLogEvent().message("arg1").logLevel(WARN).timestamp(now).verify(eventExpected)

        when:
        logger().warn("{} {}", arg1, arg2)

        then:
        singleLogEvent().message("arg1 arg2").logLevel(WARN).timestamp(now).verify(eventExpected)

        when:
        logger().warn("{} {} {}", arg1, arg2, arg3)

        then:
        singleLogEvent().message("arg1 arg2 arg3").logLevel(WARN).timestamp(now).verify(eventExpected)

        when:
        logger().warn("message", throwable)

        then:
        singleLogEvent().message("message").logLevel(WARN).timestamp(now).throwable(throwable).verify(eventExpected)

        when:
        logger().warn((Marker)null, "message")

        then:
        singleLogEvent().message("message").logLevel(WARN).timestamp(now).verify(eventExpected)

        when:
        logger().warn((Marker)null, "{}", arg1)

        then:
        singleLogEvent().message("arg1").logLevel(WARN).timestamp(now).verify(eventExpected)

        when:
        logger().warn((Marker)null, "{} {}", arg1, arg2)

        then:
        singleLogEvent().message("arg1 arg2").logLevel(WARN).timestamp(now).verify(eventExpected)

        when:
        logger().warn((Marker)null, "{} {} {}", arg1, arg2, arg3)

        then:
        singleLogEvent().message("arg1 arg2 arg3").logLevel(WARN).timestamp(now).verify(eventExpected)

        when:
        logger().warn((Marker)null, "message", throwable)

        then:
        singleLogEvent().message("message").logLevel(WARN).timestamp(now).throwable(throwable).verify(eventExpected)

        where:
        level     | eventExpected
        DEBUG     | true
        INFO      | true
        LIFECYCLE | true
        WARN      | true
        QUIET     | false
        ERROR     | false

        now = System.currentTimeMillis()
        throwable = new Throwable()
        arg1 = "arg1"
        arg2 = "arg2"
        arg3 = "arg3"
    }

    def "error calls work as expected when level is #level"() {
        given:
        globalLevel = level

        when:
        logger().error("message")

        then:
        singleLogEvent().message("message").logLevel(ERROR).timestamp(now).verify(true)

        when:
        logger().error("{}", arg1)

        then:
        singleLogEvent().message("arg1").logLevel(ERROR).timestamp(now).verify(true)

        when:
        logger().error("{} {}", arg1, arg2)

        then:
        singleLogEvent().message("arg1 arg2").logLevel(ERROR).timestamp(now).verify(true)

        when:
        logger().error("{} {} {}", arg1, arg2, arg3)

        then:
        singleLogEvent().message("arg1 arg2 arg3").logLevel(ERROR).timestamp(now).verify(true)

        when:
        logger().error("message", throwable)

        then:
        singleLogEvent().message("message").logLevel(ERROR).timestamp(now).throwable(throwable).verify(true)

        when:
        logger().error((Marker)null, "message")

        then:
        singleLogEvent().message("message").logLevel(ERROR).timestamp(now).verify(true)

        when:
        logger().error((Marker)null, "{}", arg1)

        then:
        singleLogEvent().message("arg1").logLevel(ERROR).timestamp(now).verify(true)

        when:
        logger().error((Marker)null, "{} {}", arg1, arg2)

        then:
        singleLogEvent().message("arg1 arg2").logLevel(ERROR).timestamp(now).verify(true)

        when:
        logger().error((Marker)null, "{} {} {}", arg1, arg2, arg3)

        then:
        singleLogEvent().message("arg1 arg2 arg3").logLevel(ERROR).timestamp(now).verify(true)

        when:
        logger().error((Marker)null, "message", throwable)

        then:
        singleLogEvent().message("message").logLevel(ERROR).timestamp(now).throwable(throwable).verify(true)

        where:
        level << LogLevel.values()

        now = System.currentTimeMillis()
        throwable = new Throwable()
        arg1 = "arg1"
        arg2 = "arg2"
        arg3 = "arg3"
    }

    private String stacktrace(Exception e) {
        def stream = new ByteArrayOutputStream()
        e.printStackTrace(new PrintStream(stream))
        stream.toString()
    }
}
