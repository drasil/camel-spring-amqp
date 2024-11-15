/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
package amqp.spring.camel.component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Exchange;
import org.apache.camel.Producer;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.Synchronization;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AllowedListDeserializingMessageConverter;

public class SpringAMQPProducerTest extends CamelTestSupport {
    
    @Test
    public void testCreateContext() throws Exception {
        Component component = context().getComponent("spring-amqp", SpringAMQPComponent.class);
        Assertions.assertNotNull(component);
    }
    
    @Test 
    public void restartProducer() throws Exception {
        Producer producer = context().getEndpoint("spring-amqp:fanoutExchange?durable=false&autodelete=true&exclusive=false").createProducer();
        producer.start();
        producer.stop();
    }
    
    @Test
    public void sendMessage() throws Exception {
        context().createProducerTemplate().sendBody("direct:test.z", "HELLO WORLD");
    }
    
    @Test
    public void sendAsyncMessage() throws Exception {
        context().createProducerTemplate().asyncRequestBody("direct:test.x", "HELLO WORLD");
    }
    
    @Test
    @SuppressWarnings("deprecation")
    public void sendAsyncCallbackMessage() throws Exception {
        // All asyncCallback* methods were deprecated in Camel 3.10.0 because of threading issues
        // (see CAMEL-16316 for details), but let's keep the test until the method is removed.
        context().createProducerTemplate().asyncCallbackSendBody("direct:test.w", "HELLO WORLD", new Synchronization() {
            @Override
            public void onComplete(Exchange exchange) {
                Assertions.assertNull(exchange.getException());
            }

            @Override
            public void onFailure(Exchange exchange) {
                Assertions.fail(exchange.getException() != null ? exchange.getException().getMessage() : "Failure on async callback");
            }
        });
    }
    
    @Test
    public void sendObject() throws Exception {
        context().createProducerTemplate().sendBody("direct:test.z", new ProducerTestObject());
    }
    
    @Test
    public void sendNull() throws Exception {
        context().createProducerTemplate().sendBody("direct:test.z", null);
    }
    
    @Test
    public void sendUsingDefaultExchange() throws Exception {
        context().createProducerTemplate().sendBody("direct:test.y", null);
    }
    
    @Test
    public void headerRoutingKey() throws Exception {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:test.v");
        mockEndpoint.expectedMessageCount(1);
        context().createProducerTemplate().sendBodyAndHeader("direct:test.v", new ProducerTestObject(), SpringAMQPComponent.ROUTING_KEY_HEADER, "test.v");
        mockEndpoint.assertIsSatisfied();
    }
    
    @Test
    public void uriRoutingKey() throws Exception {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:test.u");
        mockEndpoint.expectedMessageCount(1);
        context().createProducerTemplate().sendBody("direct:test.u", new ProducerTestObject());
        mockEndpoint.assertIsSatisfied();
    }
    
    @Override
    protected CamelContext createCamelContext() throws Exception {
        ConnectionFactory factory = new TestConnectionFactory();
        RabbitTemplate amqpTemplate = new RabbitTemplate(factory);
        ((AllowedListDeserializingMessageConverter)amqpTemplate.getMessageConverter())
                .addAllowedListPatterns("amqp.spring.camel.component.SpringAMQPProducerTest$ProducerTestObject");
        SpringAMQPComponent amqpComponent = new SpringAMQPComponent(factory);
        
        Map<String, AmqpTemplate> templateMap = new HashMap<>(1);
        templateMap.put(SpringAMQPComponent.DEFAULT_CONNECTION, amqpTemplate);
        amqpComponent.setAmqpTemplate(templateMap);
        
        CamelContext camelContext = super.createCamelContext();
        camelContext.addComponent("spring-amqp", amqpComponent);
        return camelContext;
    }
    
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
            	from("direct:test.y").to("spring-amqp::?durable=false&autodelete=true&exclusive=false");
                from("direct:test.z").to("spring-amqp:fanoutExchange?durable=false&autodelete=true&exclusive=false");
                from("direct:test.x").to("spring-amqp:fanoutExchange?durable=false&autodelete=true&exclusive=false");
                from("direct:test.w").to("spring-amqp:fanoutExchange?durable=false&autodelete=true&exclusive=false");
                from("direct:test.v").to("spring-amqp:topicExchange?type=topic&durable=false&autodelete=true&exclusive=false");
                from("direct:test.u").to("spring-amqp:topicExchange:test.u?durable=false&autodelete=true&exclusive=false");
                
                from("spring-amqp:topicExchange:queue.v:#.v?type=topic&durable=false&autodelete=true&exclusive=false").to("mock:test.v");
                from("spring-amqp:topicExchange:queue.u:#.u?type=topic&durable=false&autodelete=true&exclusive=false").to("mock:test.u");
                
                // we just need to bind some queue to the fanoutExchange otherwise the exchange will not be deleted after the tests
                from("spring-amqp:fanoutExchange:queue.x?type=fanout&durable=false&autodelete=true&exclusive=false").to("log:foo?level=OFF"); // trash bin
            }
        };
    }
    
    public static class ProducerTestObject implements Serializable {
        private static final long serialVersionUID = -9121162751092118857L;
        private String test;

        public String getTest() {
            return test;
        }

        public void setTest(String test) {
            this.test = test;
        }
    }
}
