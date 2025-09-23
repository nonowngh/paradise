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
    public void asyncStartLogging(JmsTemplate jmsTemplate, String ifId, String txId, String sndCd, String rcvCd) {
        String nowDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        log.info("jms start logging[{}]", txId);
        try {
            ATBUtil.startLogging(jmsTemplate, ifId, txId, null, 1, sndCd, rcvCd, nowDateTime, null);
        } catch (Exception e) {
            log.error("JMS start logging error!!!", e);
        }
    }
    
    @Async
    public void asyncEndLogging(JmsTemplate jmsTemplate, String ifId, String txId, int errorCount, String statusCd, String errorMsg) {
        log.info("jms end logging[{}]", txId);
		try {
			ATBUtil.endLogging(jmsTemplate, ifId, txId, "", errorCount, statusCd, errorMsg, null);
		} catch (Exception e) {
			log.error("JMS end logging error!!!", e);
		}
	}
}
