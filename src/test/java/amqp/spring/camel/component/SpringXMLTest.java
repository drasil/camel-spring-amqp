/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
package amqp.spring.camel.component;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Handler;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@Component
public class SpringXMLTest {
    @Resource
    protected ProducerTemplate template;
    @Resource
    protected CamelContext camelContext;
    @EndpointInject(value = "mock:testOne")
    protected MockEndpoint testOne;
    @EndpointInject(value = "mock:testTwo")
    protected MockEndpoint testTwo;
    
    @BeforeEach
    public void resetEndpoints() throws Exception {
        testOne.reset();
        testTwo.reset();
    }
    
    @Test
    public void testHappyPath() throws Exception {
        testOne.expectedMessageCount(1);
        testOne.expectedBodiesReceived("HELLO WORLD");
        template.sendBody("direct:stepOne", "HELLO WORLD");
        testOne.assertIsSatisfied();
    }
    
    @Test
    public void testHeadersExchange() throws Exception {
        testTwo.expectedMessageCount(1);
        testTwo.expectedBodiesReceived("HELLO HEADERS");
        
        Map<String, Object> headers = new HashMap<>();
        headers.put("key1", "value1");
        headers.put("key2", "value2");
        
        template.sendBodyAndHeaders("direct:stepTwo", "HELLO HEADERS", headers);
        testTwo.assertIsSatisfied();
    }
    
    @Test
    public void testRequestReply() throws Exception {
        String response = template.requestBody("direct:stepThree", "REQUEST", String.class);
        Assertions.assertEquals("RESPONSE", response);
    }
    
    @Handler
    public String handle(String body) {
        return "RESPONSE";
    }
}
