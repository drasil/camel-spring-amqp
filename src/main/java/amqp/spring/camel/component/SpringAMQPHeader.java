/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package amqp.spring.camel.component;

import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;

public class SpringAMQPHeader {
    // The (settable) AMQP Basic Properties
    public static final String CONTENT_TYPE = "contentType";
    public static final String CONTENT_ENCODING = "contentEncoding";
    public static final String PRIORITY = "priority";
    public static final String MESSAGE_ID = "messageId";
    public static final String CORRELATION_ID = "correlationId";
    public static final String APP_ID = "appId";
    public static final String REPLY_TO = "replyTo";
    public static final String EXPIRATION = "expiration";
    public static final String DELIVERY_MODE = "deliveryMode";
    public static final String TYPE = "type";
    
    public static Message setBasicPropertiesFromHeaders(Message msg, Map<String, Object> headers) {
        for (Map.Entry<String, Object> headerEntry : headers.entrySet()) {
            String headerKey = headerEntry.getKey();
            if (headerKey != null) {
                Object headerValue = headerEntry.getValue();
                String headerValueString = headerValue==null ? null : headerValue.toString();
            
                switch (headerKey) {
                    case CONTENT_ENCODING:
                        msg.getMessageProperties().setContentEncoding(headerValueString);
                        break;
                    case CONTENT_TYPE:
                        msg.getMessageProperties().setContentType(headerValueString);
                        break;
                    case MESSAGE_ID:
                        msg.getMessageProperties().setMessageId(headerValueString);
                        break;
                    case CORRELATION_ID:
                        byte[] correlationId = headerValueString != null ? headerValueString.getBytes() : null;
                        msg.getMessageProperties().setCorrelationId(correlationId);
                        msg.getMessageProperties().setCorrelationIdString(headerValueString);
                        break;
                    case APP_ID:
                        msg.getMessageProperties().setAppId(headerValueString);
                        break;
                    case EXPIRATION:
                        msg.getMessageProperties().setExpiration(headerValueString);
                        break;
                    case PRIORITY:
                        Integer priority = headerValueString != null ? Integer.parseInt(headerValueString) : null;
                        msg.getMessageProperties().setPriority(priority);
                        break;
                    case REPLY_TO:
                        msg.getMessageProperties().setReplyTo(headerValueString);
                        break;
                    case DELIVERY_MODE:
                        MessageDeliveryMode deliveryMode = headerValueString != null ? MessageDeliveryMode.fromInt(Integer.parseInt(headerValueString)) : null;
                        msg.getMessageProperties().setDeliveryMode(deliveryMode);
                        break;
                    case TYPE:
                        msg.getMessageProperties().setType(headerValueString);
                        break;
                }
            }
        }
        
        return msg;
    }
    
    /**
     * Fills Camel message headers with AMQP message properties
     * 
     * @param msg Camel message
     * @param amqpMessage AMQP message
     * @return The Camel message with headers set
     */
    public static SpringAMQPMessage setBasicPropertiesToHeaders(SpringAMQPMessage msg, Message amqpMessage) {
        msg.getHeaders().put(MESSAGE_ID, amqpMessage.getMessageProperties().getMessageId());
        msg.getHeaders().put(CORRELATION_ID, amqpMessage.getMessageProperties().getCorrelationIdString());
        if ( msg.getHeader(CORRELATION_ID, String.class)==null ) {
            byte[] correlationId = amqpMessage.getMessageProperties().getCorrelationId();
            msg.getHeaders().put(CORRELATION_ID, correlationId == null ? null : new String(correlationId));
        }
        msg.getHeaders().put(APP_ID, amqpMessage.getMessageProperties().getAppId());
        msg.getHeaders().put(CONTENT_ENCODING, amqpMessage.getMessageProperties().getContentEncoding());
        msg.getHeaders().put(CONTENT_TYPE, amqpMessage.getMessageProperties().getContentType());
        msg.getHeaders().put(EXPIRATION, amqpMessage.getMessageProperties().getExpiration());
        msg.getHeaders().put(PRIORITY, amqpMessage.getMessageProperties().getPriority());
        msg.getHeaders().put(REPLY_TO, amqpMessage.getMessageProperties().getReplyTo());
        MessageDeliveryMode deliveryMode = amqpMessage.getMessageProperties().getReceivedDeliveryMode();
        msg.getHeaders().put(DELIVERY_MODE, deliveryMode == null ? null : MessageDeliveryMode.toInt(deliveryMode));
        msg.getHeaders().put(TYPE, amqpMessage.getMessageProperties().getType());

        return msg;
    }
    
    public static Message copyHeaders(Message msg, Map<String, Object> headers) {
        for(Map.Entry<String, Object> headerEntry : headers.entrySet()) {

            // headers used for setting basic properties and routing key are skipped
            if ( !CONTENT_ENCODING.equals(headerEntry.getKey()) &&
                    !CONTENT_TYPE.equals(headerEntry.getKey()) &&
                    !MESSAGE_ID.equals(headerEntry.getKey()) &&
                    !CORRELATION_ID.equals(headerEntry.getKey()) &&
                    !APP_ID.equals(headerEntry.getKey()) &&
                    !EXPIRATION.equals(headerEntry.getKey()) &&
                    !PRIORITY.equals(headerEntry.getKey()) &&
                    !REPLY_TO.equals(headerEntry.getKey()) &&
                    !DELIVERY_MODE.equals(headerEntry.getKey()) &&
                    !TYPE.equals(headerEntry.getKey()) &&
                    !SpringAMQPComponent.ROUTING_KEY_HEADER.equals(headerEntry.getKey()) &&
                    !SpringAMQPComponent.EXCHANGE_NAME_HEADER.equals(headerEntry.getKey()) &&
                    !msg.getMessageProperties().getHeaders().containsKey(headerEntry.getKey())) {
                msg.getMessageProperties().setHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        
        return msg;
    }
    
    public static SpringAMQPMessage copyHeaders(SpringAMQPMessage msg, Map<String, Object> headers) {
        for(Map.Entry<String, Object> headerEntry : headers.entrySet()) {
            if ( !SpringAMQPMessage.EXCHANGE_PATTERN.equals(headerEntry.getKey()) ) {
                msg.setHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        
        return msg;
    }
}
