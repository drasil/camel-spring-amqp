/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
package amqp.spring.camel.component;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

public class SpringAMQPHeaderTest {
    @Test
    public void fromBasicProperties() throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("NotSecret", "Popcorn");
        org.springframework.amqp.core.Message message = new Message(new byte[]{}, properties);
        message.getMessageProperties().setPriority(1);
        message.getMessageProperties().setReplyTo("BuzzSaw");
        message.getMessageProperties().setCorrelationId("corrId");
        
        SpringAMQPMessage camelMessage = SpringAMQPHeader.setBasicPropertiesToHeaders(new SpringAMQPMessage(new DefaultCamelContext()), message);
        Assertions.assertNull(camelMessage.getHeader("NotSecret"));
        Assertions.assertEquals(1, camelMessage.getHeader(SpringAMQPHeader.PRIORITY));
        Assertions.assertEquals("BuzzSaw", camelMessage.getHeader(SpringAMQPHeader.REPLY_TO));
        Assertions.assertEquals("corrId", camelMessage.getHeader(SpringAMQPHeader.CORRELATION_ID));
    }
    
    @Test
    public void toBasicProperties() throws Exception {
        CamelContext context = new DefaultCamelContext();
        SpringAMQPMessage camelMessage = new SpringAMQPMessage(context);
        camelMessage.setHeader("Secret", "My Secret");
        camelMessage.setHeader(SpringAMQPHeader.PRIORITY, 1);
        camelMessage.setHeader(SpringAMQPHeader.REPLY_TO, "BuzzSaw");
        camelMessage.setHeader(SpringAMQPHeader.CORRELATION_ID, "corrId");
        camelMessage.setHeader(SpringAMQPHeader.DELIVERY_MODE, null);
        
        Exchange exchange = new DefaultExchange(context);
        exchange.setIn(camelMessage);
        
        Message message = new Message(new byte[]{}, new MessageProperties());
        message = SpringAMQPHeader.setBasicPropertiesFromHeaders(message, camelMessage.getHeaders());
        Assertions.assertNull(message.getMessageProperties().getHeaders().get("Secret"));
        Assertions.assertEquals(Integer.valueOf(1), message.getMessageProperties().getPriority());
        Assertions.assertEquals("BuzzSaw", message.getMessageProperties().getReplyTo());
        Assertions.assertEquals("corrId", message.getMessageProperties().getCorrelationId());
    }
    
    @Test
    public void copyAMQPHeaders() throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("NotSecret", "Popcorn");
        org.springframework.amqp.core.Message message = new Message(new byte[]{}, properties);
        message.getMessageProperties().setReplyTo("BuzzSaw");
        
        SpringAMQPMessage camelMessage = SpringAMQPHeader.copyHeaders(new SpringAMQPMessage(new DefaultCamelContext()), message.getMessageProperties().getHeaders());
        Assertions.assertEquals("Popcorn", camelMessage.getHeader("NotSecret"));
        Assertions.assertNull(camelMessage.getHeader(SpringAMQPHeader.REPLY_TO));
    }
    
    @Test
    public void copyCamelHeaders() throws Exception {
        CamelContext context = new DefaultCamelContext();
        SpringAMQPMessage camelMessage = new SpringAMQPMessage(context);
        camelMessage.setHeader("Secret", "My Secret");
        camelMessage.setHeader(SpringAMQPHeader.REPLY_TO, "BuzzSaw");
        
        Exchange exchange = new DefaultExchange(context);
        exchange.setIn(camelMessage);
        
        Message message = new Message(new byte[]{}, new MessageProperties());
        message = SpringAMQPHeader.copyHeaders(message, camelMessage.getHeaders());
        Assertions.assertEquals("My Secret", message.getMessageProperties().getHeaders().get("Secret"));
        Assertions.assertNull(message.getMessageProperties().getReplyTo());
    }
}
