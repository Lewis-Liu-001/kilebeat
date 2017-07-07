package com.skillbill.at.akka;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.skillbill.at.akka.dto.KafkaEndPointConfiuration;
import com.skillbill.at.akka.dto.KafkaEndPointFailed;
import com.skillbill.at.akka.dto.NewLineEvent;
import com.skillbill.at.guice.GuiceAbstractActor;

import akka.actor.ActorRef;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KafkaEndpointActor extends GuiceAbstractActor {
	
	private final Producer<String, String> producer;
	private final ObjectMapper om;
	private KafkaEndPointConfiuration conf;
	
	@Inject
	public KafkaEndpointActor() {		
		
        Properties configProperties = new Properties();
        configProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        configProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.ByteArraySerializer");
        configProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
		
        //inject me please !!?
        producer = new KafkaProducer<String, String>(configProperties);
        
        om = new ObjectMapper();
        
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(KafkaEndPointConfiuration.class, c -> conf = c)
			.match(NewLineEvent.class, s -> send(s))
			.build();
	}
	
	@Override
	public void postStop() throws Exception {
		super.postStop();
		
		LOGGER.info("############################################ " + getSelf().path());
		
		producer.close();
		
		getContext().parent().tell(new KafkaEndPointFailed(conf), ActorRef.noSender());
	}
	
	@Override
	public void preStart() throws Exception {
		super.preStart();
		
		LOGGER.info("**************************************** " + getSelf().path());
	}

	private void send(NewLineEvent s) {
		LOGGER.info("[row@{}] {}", getSelf().path(), s);
		
		try {
			final RecordMetadata recordMetadata = producer.send(
				new ProducerRecord<String, String>("topicName", om.writeValueAsString(s))
			).get();
			
			long offset = recordMetadata.offset();			
			LOGGER.info("offset is {}", offset);			
		} catch (Exception e) {
			LOGGER.error("", e);
		}		
	}		
}	