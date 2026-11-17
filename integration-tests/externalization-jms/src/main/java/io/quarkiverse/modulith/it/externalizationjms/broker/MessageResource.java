package io.quarkiverse.modulith.it.externalizationjms.broker;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import org.jboss.resteasy.reactive.RestResponse;

/**
 * Receives the messages the application sent, so that tests over HTTP can check them. The typed
 * {@link RestResponse} lets Quarkus register {@link ReceivedMessage} for JSON serialization in a native image.
 */
@Path("/messages")
public class MessageResource {

    private static final String DEFAULT_TIMEOUT_MILLIS = "5000";
    private static final String QUOTE = "'";

    @Inject
    ConnectionFactory connections;

    @GET
    @Path("/{queue}/{orderId}")
    public RestResponse<ReceivedMessage> receive(@PathParam("queue") String queue,
            @PathParam("orderId") String orderId,
            @QueryParam("timeout") @DefaultValue(DEFAULT_TIMEOUT_MILLIS) long timeout) throws JMSException {
        if (orderId.contains(QUOTE)) {
            throw new BadRequestException("An order id with a quote would break the message selector: " + orderId);
        }
        try (JMSContext context = connections.createContext();
                JMSConsumer consumer = context.createConsumer(context.createQueue(queue),
                        "orderId = " + QUOTE + orderId + QUOTE)) {
            Message message = consumer.receive(timeout);
            if (Objects.isNull(message)) {
                return RestResponse.noContent();
            }
            Map<String, Object> properties = new TreeMap<>();
            for (Object property : Collections.list((Enumeration<?>) message.getPropertyNames())) {
                String name = property.toString();
                if (!name.startsWith("JMS") && !name.startsWith("_")) {
                    properties.put(name, message.getObjectProperty(name));
                }
            }
            return RestResponse.ok(new ReceivedMessage(message.getJMSMessageID(),
                    message.getJMSDestination().toString(), ((TextMessage) message).getText(), properties));
        }
    }
}
