/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package amqp.spring.camel.component;

//Not a super-great load test, but something to get the ball rolling

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import javax.annotation.Resource;
import org.apache.camel.CamelContext;
import org.apache.camel.Handler;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@Component
public class ContrivedLoadTest {
    private static transient final Logger LOG = LoggerFactory.getLogger(ContrivedLoadTest.class);
    
    @Resource
    protected ProducerTemplate template;
    @Resource
    protected CamelContext camelContext;
    
    @Test
    public void testSynchronous() throws Exception {
        final int messageCount = 1000;
        int received = 0;
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(100);
        List<Future<String>> futures = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        for(int i=0; i < messageCount; ++i)
            futures.add(executorService.submit(new SynchronousRequestor(this.template)));
        LOG.info("Time to submit synchronous messages: {}", (System.currentTimeMillis() - startTime) / 1000.0f);

        startTime = System.currentTimeMillis();
        for(Future<String> future : futures) {
            String response = future.get();
            if ("RESPONSE".equals(response)) {
                ++received;
            } else {
                LOG.warn("Unexpected response '{}'", response);
            }
        }
        float elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0f;
        int maxPoolSize = this.camelContext.getExecutorServiceManager().getDefaultThreadPoolProfile().getMaxPoolSize();
        LOG.info("Time to receive synchronous messages: {}", elapsedTime);
        
        Assertions.assertEquals(messageCount, received);
        //Assuming 1 second delay per message, elapsed time shouldn't exceed the number of messages sent 
        //divided by the number of messages that can be simultaneously consumed.
        Assertions.assertTrue(elapsedTime < (messageCount / (double) maxPoolSize) + 1,
                String.format("Possible performance issue: %d messages took %f seconds with %d consumers", messageCount, elapsedTime, maxPoolSize));
    }
    
    @Test
    public void testAsynchronous() throws Exception {
        final int messageCount = 1000;
        int received = 0;
        List<Future<String>> futures = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        for(int i=0; i < messageCount; ++i)
            futures.add(this.template.asyncRequestBody("direct:sync", "HELLO WORLD", String.class));
        LOG.info("Time to submit asynchronous messages: {}", (System.currentTimeMillis() - startTime) / 1000.0f);

        startTime = System.currentTimeMillis();
        for(Future<String> future : futures) {
            String response = future.get(10, TimeUnit.SECONDS);
            if("RESPONSE".equals(response)) {
                ++received;
            } else {
                LOG.warn("Unexpected response '{}'", response);
            }
        }
        float elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0f;
        int maxPoolSize = this.camelContext.getExecutorServiceManager().getDefaultThreadPoolProfile().getMaxPoolSize();
        LOG.info("Time to receive asynchronous messages: {}", elapsedTime);
        
        Assertions.assertEquals(messageCount, received);
        //Assuming 1 second delay per message, elapsed time shouldn't exceed the number of messages sent 
        //divided by the number of messages that can be simultaneously consumed.
        Assertions.assertTrue(elapsedTime < (messageCount / (double) maxPoolSize) + 1,
                String.format("Possible performance issue: %d messages took %f seconds with %d consumers", messageCount, elapsedTime, maxPoolSize));
    }
    
    @Handler
    public String handle(String body) {
        try {
            Thread.sleep(1000);
        } catch(InterruptedException e) {
            LOG.error("Error when attempting to sleep in handler", e);
        }
        return "RESPONSE";
    }
    
    private static class SynchronousRequestor implements Callable<String> {
        private ProducerTemplate template;
        public SynchronousRequestor(ProducerTemplate template) {
            this.template = template;
        }
        
        @Override
        public String call() throws Exception {
            return template.requestBody("direct:sync", "HELLO WORLD", String.class);
        }
    }
}
