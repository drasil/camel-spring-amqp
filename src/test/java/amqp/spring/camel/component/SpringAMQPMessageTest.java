/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
package amqp.spring.camel.component;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.AbstractMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

public class SpringAMQPMessageTest {
    @Test
    public void testExchangePattern() throws Exception {
        CamelContext context = new DefaultCamelContext();
        org.apache.camel.Message camelMessage = new DefaultMessage(context);
        Exchange exchange = new DefaultExchange(context, ExchangePattern.InOut);
        exchange.setIn(camelMessage);
        
        MessageProperties properties = new MessageProperties();
        org.springframework.amqp.core.Message amqpMessage = new org.springframework.amqp.core.Message("Testing".getBytes(), properties);
        
        amqpMessage = new SpringAMQPMessage.HeadersPostProcessor(camelMessage).postProcessMessage(amqpMessage);
        ExchangePattern exchangePattern = SpringAMQPMessage.getExchangePattern(amqpMessage);
        Assertions.assertEquals(exchange.getPattern(), exchangePattern);
    }
    
    @Test
    public void fromAMQP() throws Exception {
        String body = "Test Message";
        MessageConverter msgConverter = new StringMessageConverter();
        MessageProperties properties = new MessageProperties();
        properties.setHeader("NotSecret", "Popcorn");
        org.springframework.amqp.core.Message message = new org.springframework.amqp.core.Message(body.getBytes(), properties);
        
        SpringAMQPMessage camelMessage = SpringAMQPMessage.fromAMQPMessage(new DefaultCamelContext(), msgConverter, message);
        Assertions.assertEquals(body, camelMessage.getBody(String.class));
        Assertions.assertEquals("Popcorn", camelMessage.getHeader("NotSecret"));
    }
    
    @Test
    public void toAMQP() throws Exception {
        CamelContext context = new DefaultCamelContext();
        MessageConverter msgConverter = new StringMessageConverter();
        
        SpringAMQPMessage camelMessage = new SpringAMQPMessage(context);
        camelMessage.setBody("Test Message 2");
        camelMessage.setHeader("Secret", "My Secret");
        
        Exchange exchange = new DefaultExchange(context);
        exchange.setIn(camelMessage);
        
        org.springframework.amqp.core.Message message = camelMessage.toAMQPMessage(msgConverter);
        Assertions.assertEquals("Test Message 2", new String(message.getBody()));
        Assertions.assertEquals("My Secret", message.getMessageProperties().getHeaders().get("Secret"));
    }
    
    private static class StringMessageConverter extends AbstractMessageConverter {
        @Override
        protected org.springframework.amqp.core.Message createMessage(Object object, MessageProperties messageProperties) {
            return new org.springframework.amqp.core.Message(((String) object).getBytes(), messageProperties);
        }

        @Override
        public Object fromMessage(org.springframework.amqp.core.Message message) throws MessageConversionException {
            return new String(message.getBody());
        }
    }
}
