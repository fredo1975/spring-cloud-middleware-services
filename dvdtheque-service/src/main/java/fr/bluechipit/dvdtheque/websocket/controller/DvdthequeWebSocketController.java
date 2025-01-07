package fr.bluechipit.dvdtheque.websocket.controller;

import jms.model.JmsStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import fr.bluechipit.dvdtheque.dao.domain.Film;

@Controller
public class DvdthequeWebSocketController {
	protected Logger logger = LoggerFactory.getLogger(DvdthequeWebSocketController.class);
	@Autowired
	SimpMessagingTemplate simpMessagingTemplate;

	@MessageMapping("/dvdtheque-ws")
	public void onReceiveMessage(JmsStatusMessage<Film> jmsStatusMessage) {
		logger.info("onReceiveMessage jmsStatusMessage="+jmsStatusMessage.toString()); 
		this.simpMessagingTemplate.convertAndSend("/topic/", jmsStatusMessage);
	}
	@SubscribeMapping("/topic")
	public void onSendMessage(){
		logger.info("onSendMessage"); 
	}
	@MessageExceptionHandler
	public String handleException(Throwable exception) {
		simpMessagingTemplate.convertAndSend("/errors", exception.getMessage());
		return exception.getMessage();
	}
}
