package mb.fw.paradise.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.util.ATBUtil;

@Slf4j
@Service
public class LoggingService {

	@Async
	public void asyncStartLogging(JmsTemplate jmsTemplate, String interfaceId, String transactionId,
			String sendSystemdCode, String receiveSystemCode, int totalCount) {
		String nowDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
		log.debug("jms start logging[{}]", transactionId);
		try {
			ATBUtil.startLogging(jmsTemplate, interfaceId, transactionId, null, totalCount, sendSystemdCode, receiveSystemCode,
					nowDateTime, null);
		} catch (Exception e) {
			log.error("JMS start logging error!!!", e);
		}
	}

	@Async
	public void asyncEndLogging(JmsTemplate jmsTemplate, String interfaceId, String transactionId, int errorCount, String statusCode, String statusMessage) {
		log.debug("jms end logging[{}]", transactionId);
		try {
			ATBUtil.endLogging(jmsTemplate, interfaceId, transactionId, "",
					errorCount, statusCode, statusMessage, null);
		} catch (Exception e) {
			log.error("JMS end logging error!!!", e);
		}
	}
	
}
