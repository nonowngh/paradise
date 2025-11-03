package mb.fw.paradise.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.constants.ESBApiHeaderConstants;
import mb.fw.paradise.constants.ESBStatusConstants;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.util.HttpHeaderUtil;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ExceptionService {

	private final APIService apiService;

	public ExceptionService(APIService apiService) {
		this.apiService = apiService;
	}

	public Mono<String> receiveHandlerExceptionProcess(Throwable e, String interfaceId, String transactionId,
			int dataCount, String callBackPath) {
		return apiService.callGatewayForResult(APIResponseMessage.builder().interfaceId(interfaceId)
				.transactionId(transactionId).statusCode(ESBStatusConstants.FAIL)
				.statusMessage(Base64.getEncoder().encodeToString(e.getMessage().getBytes(StandardCharsets.UTF_8)))
				.dataCount(dataCount).build(), callBackPath);
	}

	public APIResponseMessage syncExceptionProcess(Throwable e, HttpHeaders headers) {
		log.error("Handler error -> ", e);
		return APIResponseMessage.builder()
				.interfaceId(HttpHeaderUtil.getHeaderIgnoreCase(headers, ESBApiHeaderConstants.INTERFACE_ID))
				.transactionId(HttpHeaderUtil.getHeaderIgnoreCase(headers, ESBApiHeaderConstants.TRANSACTION_ID))
				.statusCode(ESBStatusConstants.FAIL).statusMessage(e.getMessage())
				.dataCount(HttpHeaderUtil.getIntHeaderIgnoreCase(headers, ESBApiHeaderConstants.DATA_COUNT)).build();
	}
}
