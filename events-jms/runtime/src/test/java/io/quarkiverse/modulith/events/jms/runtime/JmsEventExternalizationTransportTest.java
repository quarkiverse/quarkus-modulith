package io.quarkiverse.modulith.events.jms.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.JMSProducer;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import io.quarkiverse.modulith.events.RoutingTarget;

class JmsEventExternalizationTransportTest {

    private static final String BODY = "{\"event\":\"o-1\"}";
    private static final Duration SEND_TIMEOUT = Duration.ofMillis(50);

    private final ConnectionFactory connections = mock(ConnectionFactory.class);
    private final JMSContext context = mock(JMSContext.class);
    private final JMSProducer producer = mock(JMSProducer.class);
    private final Queue queue = mock(Queue.class);
    private final Topic topic = mock(Topic.class);
    private final TextMessage message = mock(TextMessage.class);
    private final CountingExecutor executor = new CountingExecutor();

    @BeforeEach
    void stubTheBroker() throws JMSException {
        when(connections.createContext()).thenReturn(context);
        when(context.createQueue("orders")).thenReturn(queue);
        when(context.createTopic("orders")).thenReturn(topic);
        when(context.createTextMessage(BODY)).thenReturn(message);
        when(context.createProducer()).thenReturn(producer);
        when(message.getJMSMessageID()).thenReturn("ID:1");
    }

    private JmsEventExternalizationTransport transport(JmsDestinationType type) {
        return new JmsEventExternalizationTransport(connections, new FixedSerializer(), type, SEND_TIMEOUT, executor);
    }

    @Test
    void sendsATextMessageToTheQueueWithTheHeadersAsProperties() throws JMSException {
        // arrange
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("orderId", "o-1");
        headers.put("quantity", 3);

        // act
        CompletableFuture<?> sent = transport(JmsDestinationType.QUEUE)
                .externalize("o-1", RoutingTarget.forTarget("orders").andKey("ignored"), headers).toCompletableFuture();

        // assert
        assertThat(sent.getNow(null)).isEqualTo("ID:1");
        InOrder order = inOrder(message, producer, context);
        order.verify(message).setObjectProperty("orderId", "o-1");
        order.verify(message).setObjectProperty("quantity", 3);
        order.verify(producer).send(queue, message);
        order.verify(context).close();
        assertThat(executor.executed()).isEqualTo(1);
    }

    @Test
    void sendsToATopicWhenConfigured() {
        // act
        CompletableFuture<?> sent = transport(JmsDestinationType.TOPIC)
                .externalize("o-1", RoutingTarget.forTarget("orders"), Map.of())
                .toCompletableFuture();

        // assert
        assertThat(sent.getNow(null)).isEqualTo("ID:1");
        verify(producer).send(topic, message);
    }

    @Test
    void failsWhenTheBrokerRejectsTheMessage() {
        // arrange
        JMSRuntimeException down = new JMSRuntimeException("broker down");
        when(producer.send(queue, message)).thenThrow(down);

        // act
        CompletableFuture<?> sent = transport(JmsDestinationType.QUEUE)
                .externalize("o-1", RoutingTarget.forTarget("orders"), Map.of()).toCompletableFuture();

        // assert
        assertThat(sent).isCompletedExceptionally();
        assertThat(sent.exceptionNow()).isSameAs(down);
        verify(context).close();
    }

    @Test
    void wrapsCheckedJmsFailures() throws JMSException {
        // arrange
        JMSException failure = new JMSException("no id", "E42");
        when(message.getJMSMessageID()).thenThrow(failure);

        // act
        CompletableFuture<?> sent = transport(JmsDestinationType.QUEUE)
                .externalize("o-1", RoutingTarget.forTarget("orders"), Map.of()).toCompletableFuture();

        // assert
        assertThat(sent.exceptionNow()).isInstanceOf(JMSRuntimeException.class)
                .hasMessage("Could not send an event of type java.lang.String to the JMS QUEUE orders")
                .hasCause(failure);
        assertThat(((JMSRuntimeException) sent.exceptionNow()).getErrorCode()).isEqualTo("E42");
    }

    @Test
    void failsBeforeConnectingWhenAHeaderNameIsInvalid() {
        // act
        CompletableFuture<?> sent = transport(JmsDestinationType.QUEUE)
                .externalize("o-1", RoutingTarget.forTarget("orders"), Map.of("order-id", "o-1"))
                .toCompletableFuture();

        // assert
        assertThat(sent.exceptionNow()).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The header order-id is not a valid JMS property name: use a Java identifier that is not a"
                        + " message selector keyword");
        assertThat(executor.executed()).isEqualTo(1);
        verifyNoInteractions(connections);
    }

    @Test
    void failsWhenTheProviderDoesNotReturnFromSendInTime() {
        // arrange
        JmsEventExternalizationTransport transport = new JmsEventExternalizationTransport(connections,
                new FixedSerializer(), JmsDestinationType.QUEUE, SEND_TIMEOUT, command -> {
                });

        // act
        Throwable thrown = catchThrowable(() -> transport.externalize("o-1", RoutingTarget.forTarget("orders"),
                Map.of()).toCompletableFuture().get(2, TimeUnit.SECONDS));

        // assert
        assertThat(thrown).isInstanceOf(ExecutionException.class).cause().isInstanceOf(TimeoutException.class)
                .hasMessage("The JMS provider did not return from sending to the QUEUE orders within PT0.05S"
                        + " (quarkus.modulith.events.jms.send-timeout)");
        verifyNoInteractions(connections);
    }
}
