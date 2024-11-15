/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package amqp.spring.camel.component;

import java.util.HashMap;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class SpringAMQPComponent extends DefaultComponent {
    private static transient final Logger LOG = LoggerFactory.getLogger(SpringAMQPComponent.class);
    
    private Map<String, ConnectionFactory> connectionFactory;
    private Map<String, AmqpTemplate> amqpTemplate;
    private Map<String, AmqpAdmin> amqpAdministration;
    public static final String ROUTING_KEY_HEADER = "ROUTING_KEY";
    public static final String DEFAULT_CONNECTION = "DefaultConnection";
    public static final String CONNECTION = "connection";
    public static final String EXCHANGE_NAME_HEADER = "EXCHANGE_NAME";
    
    public SpringAMQPComponent() {
        this(new CachingConnectionFactory());
    }
    
    public SpringAMQPComponent(CamelContext context) {
        super(context);

        //Attempt to load a connection factory from the registry
        if(this.connectionFactory == null) {
            this.connectionFactory = context.getRegistry().findByTypeWithName(ConnectionFactory.class);
            if(this.connectionFactory != null && !this.connectionFactory.isEmpty()) {
                for(Map.Entry<String, ConnectionFactory> connection : this.connectionFactory.entrySet()){
                    LOG.info("Found AMQP ConnectionFactory in registry for {}", connection.getValue().getHost());
                }
            }
        }

        if(this.connectionFactory == null) {
            LOG.error("Cannot find a connection factory!");
        }
    }

    public SpringAMQPComponent(ConnectionFactory connectionFactory) {
        this.connectionFactory = new HashMap<>();
        this.connectionFactory.put(DEFAULT_CONNECTION, connectionFactory);
    }

    public SpringAMQPComponent(Map<String, ConnectionFactory> connectionFactories) {
        this.connectionFactory = connectionFactories;
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        String connection = parameters.get(CONNECTION) != null ? (String) parameters.get(CONNECTION) : connectionFactory.keySet().iterator().next();
        SpringAMQPEndpoint endpoint = new SpringAMQPEndpoint(this, uri, remaining,
                getAmqpTemplate().get(connection), getAmqpAdministration().get(connection));
        setProperties(endpoint, parameters);
        return endpoint;
    }

    public Map<String, ConnectionFactory> getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(Map<String, ConnectionFactory> connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public Map<String, AmqpAdmin> getAmqpAdministration() {
        if(this.amqpAdministration == null && getCamelContext() != null && getCamelContext().getRegistry() != null) {
            //Attempt to load an administration connection from the registry
            this.amqpAdministration = new HashMap<>();
            Map<String, AmqpAdmin> adminMap = getCamelContext().getRegistry().findByTypeWithName(AmqpAdmin.class);
            for(AmqpAdmin admin : adminMap.values()){
                CachingConnectionFactory adminConnection = (CachingConnectionFactory)((RabbitAdmin)admin).getRabbitTemplate().getConnectionFactory();
                for(Map.Entry<String, ConnectionFactory> connection : this.connectionFactory.entrySet()){
                    if(identicalConnectionFactories(adminConnection, connection.getValue())){
                        this.amqpAdministration.put(connection.getKey(), admin);
                        break;
                    }
                }
            }
            if(this.amqpAdministration != null && !this.amqpAdministration.isEmpty()){
                LOG.info("Found AMQP Administrator in registry");
            }
        }
        
        if(this.amqpAdministration == null || this.amqpAdministration.isEmpty()) {
            //Attempt to construct an AMQP Adminstration instance
            this.amqpAdministration = new HashMap<>();
            this.amqpAdministration.put(DEFAULT_CONNECTION, new RabbitAdmin(this.connectionFactory.values().iterator().next()));
            LOG.info("Created new AMQP Administration instance");
        }
        
        return this.amqpAdministration;
    }

    public void setAmqpAdministration(Map<String, AmqpAdmin> amqpAdministration) {
        this.amqpAdministration = amqpAdministration;
    }

    public Map<String, AmqpTemplate> getAmqpTemplate() {
        if(this.amqpTemplate == null && getCamelContext() != null && getCamelContext().getRegistry() != null) {
            //Attempt to load an AMQP template from the registry
            this.amqpTemplate = new HashMap<>();
            Map<String, AmqpTemplate> templateMap = getCamelContext().getRegistry().findByTypeWithName(AmqpTemplate.class);
            for(AmqpTemplate template : templateMap.values()){
                CachingConnectionFactory adminConnection = (CachingConnectionFactory)((RabbitTemplate) template).getConnectionFactory();
                for(Map.Entry<String, ConnectionFactory> connection : this.connectionFactory.entrySet()){
                    if(identicalConnectionFactories(adminConnection, connection.getValue())){
                        this.amqpTemplate.put(connection.getKey(), template);
                        break;
                    }
                }
            }
            if(this.amqpTemplate != null && !this.amqpTemplate.isEmpty()){
                LOG.info("Found AMQP Template in registry");
            }
        }
        
        if(this.amqpTemplate == null || this.amqpTemplate.isEmpty()) {
            //Attempt to construct an AMQP template
            this.amqpTemplate = new HashMap<>();
            this.amqpTemplate.put(DEFAULT_CONNECTION, new RabbitTemplate(this.connectionFactory.values().iterator().next()));
            LOG.info("Created new AMQP Template");
        }
        
        return this.amqpTemplate;
    }

    public void setAmqpTemplate(Map<String, AmqpTemplate> amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    } 
    
    public static Throwable findRootCause(Throwable t) {
        return t.getCause()==null ? t : findRootCause(t.getCause());
    }

    private boolean identicalConnectionFactories(ConnectionFactory cf, ConnectionFactory candidateCF) {
      String host = candidateCF.getHost();
      String vhost = candidateCF.getVirtualHost();
      int port = candidateCF.getPort();
      return cf.getHost().equals(host) && cf.getVirtualHost().equals(vhost) && cf.getPort() == port;
    }
}
