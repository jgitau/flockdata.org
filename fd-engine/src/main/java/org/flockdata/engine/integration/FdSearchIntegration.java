package org.flockdata.engine.integration;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.NullChannel;

/**
 * Created by mike on 12/02/16.
 */
@Configuration
@IntegrationComponentScan
@Profile({"integration","production"})
public class FdSearchIntegration {

    @Autowired
    AmqpRabbitConfig rabbitConfig;

    @Autowired
    Exchanges exchanges;

    @Bean
    @ServiceActivator(inputChannel = "syncSearchDocs")
    public AmqpOutboundEndpoint fdSearchAMQPOutbound(AmqpTemplate amqpTemplate) {
        AmqpOutboundEndpoint outbound = new AmqpOutboundEndpoint(amqpTemplate);
        outbound.setLazyConnect(rabbitConfig.getAmqpLazyConnect());
        outbound.setRoutingKey(exchanges.getSearchBinding());
        outbound.setExchangeName(exchanges.getSearchExchange());
        outbound.setExpectReply(false);
        outbound.setConfirmAckChannel(new NullChannel());// NOOP
        //outbound.setConfirmAckChannel();
        return outbound;

    }

    @Bean
    @ServiceActivator(inputChannel = "writeKvContent")
    public AmqpOutboundEndpoint fdWriteKvContent(AmqpTemplate amqpTemplate){
        AmqpOutboundEndpoint outbound = new AmqpOutboundEndpoint(amqpTemplate);
        outbound.setLazyConnect(rabbitConfig.getAmqpLazyConnect());
        outbound.setRoutingKey(exchanges.getStoreBinding());
        outbound.setExchangeName(exchanges.getStoreExchange());
        outbound.setExpectReply(false);
        outbound.setConfirmAckChannel(new NullChannel());// NOOP
        //outbound.setConfirmAckChannel();
        return outbound;

    }

    @Bean
    @ServiceActivator(inputChannel = "writeEntityContent")
    public AmqpOutboundEndpoint fdWriteEntityContent(AmqpTemplate amqpTemplate){
        AmqpOutboundEndpoint outbound = new AmqpOutboundEndpoint(amqpTemplate);
        outbound.setLazyConnect(rabbitConfig.getAmqpLazyConnect());
        outbound.setRoutingKey(exchanges.getTrackBinding());
        outbound.setExchangeName(exchanges.getTrackExchange());
        DefaultAmqpHeaderMapper headerMapper = new DefaultAmqpHeaderMapper();
        headerMapper.setRequestHeaderNames("apiKey");
        outbound.setHeaderMapper(headerMapper);
        outbound.setExpectReply(false);
        outbound.setConfirmAckChannel(new NullChannel());// NOOP
        return outbound;

    }

//    @Bean
//    public AmqpInboundGateway inbound(SimpleMessageListenerContainer listenerContainer,
//                                      @Qualifier("doTrackEntity") MessageChannel channel) {
//        AmqpInboundGateway gateway = new AmqpInboundGateway(listenerContainer);
//        gateway.setRequestChannel(channel);
//        gateway.setReplyChannel(trackResult());
//
//        gateway.setDefaultReplyTo("bar");
//        return gateway;
    //    public AmqpInboundGateway fdEntityWrite (){
//        <int-amqp:inbound-gateway id="entityWrite"
//        queue-names="${fd-track.queue}"
//        connection-factory="connectionFactory"
//        shutdown-timeout="3000"
//        task-executor="fd-track"
//        mapped-request-headers="*"
//        mapped-reply-headers="*"
//        error-channel="entityErrors"
//        message-converter="jsonToEntityInput"
//        concurrent-consumers="${fd-track.concurrentConsumers}"
//        prefetch-count="${fd-track.prefetchCount}"
//                />
//    }

//    }
}
