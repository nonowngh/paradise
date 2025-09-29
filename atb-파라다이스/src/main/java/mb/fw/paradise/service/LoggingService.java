package mb.fw.paradise.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.util.ATBUtil;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.APIResponseMessage;

@Slf4j
@Service
public class LoggingService {

	@Async
	public void asyncStartLogging(JmsTemplate jmsTemplate, APIRequestMessage message) {
		String nowDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
		log.debug("jms start logging[{}]", message.getTransactionId());
		try {
			ATBUtil.startLogging(jmsTemplate, message.getInterfaceId(), message.getTransactionId(), null, 1, message.getSendSystemCode(), message.getReceiveSystemCode(), nowDateTime, null);
		} catch (Exception e) {
			log.error("JMS start logging error!!!", e);
		}
	}

	@Async
	public void asyncEndLogging(JmsTemplate jmsTemplate, APIResponseMessage message) {
		log.debug("jms end logging[{}]", message.getTransactionId());
		try {
			ATBUtil.endLogging(jmsTemplate, message.getInterfaceId(), message.getTransactionId(), "", message.getErrorDataCount(), message.getStatusCode(), message.getStatusMessage(), null);
		} catch (Exception e) {
			log.error("JMS end logging error!!!", e);
		}
	}
}
