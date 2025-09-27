package mb.fw.paradise.service;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.constants.ESBStatusConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.APIResponseMessage;

@Slf4j
@Service
public class ExceptionService {

	private final APIService apiService;

	public ExceptionService(APIService apiService) {
		this.apiService = apiService;
	}

	public void exceptionProcess(Throwable e, APIRequestMessage request) {

		log.error("Handler error -> ", e);

		apiService.callGatewayForResult(APIResponseMessage.builder().interfaceId(request.getInterfaceId())
				.transactionId(request.getTransactionId()).statusCode(ESBStatusConstants.FAIL)
				.statusMessage(e.getMessage()).build(), request.getCallBackPath());
	}
}
