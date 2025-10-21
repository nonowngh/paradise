package mb.fw.paradise.service;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.constants.ESBApiHeaderConstants;
import mb.fw.paradise.constants.ESBStatusConstants;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.util.HttpHeaderUtil;

@Slf4j
@Service
public class ExceptionService {

	private final APIService apiService;

	public ExceptionService(APIService apiService) {
		this.apiService = apiService;
	}

	public void receiveHandlerExceptionProcess(Throwable e, HttpHeaders headers) {
		log.error("Handler error -> ", e);
		apiService.callGatewayForResult(APIResponseMessage.builder()
				.interfaceId(HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.INTERFACE_ID))
				.transactionId(HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.TRANSACTION_ID))
				.statusCode(ESBStatusConstants.FAIL).statusMessage(e.getMessage())
				.totalDataCount(Integer.valueOf(HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.TOTAL_COUNT)))
				.errorDataCount(Integer.valueOf(HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.TOTAL_COUNT)))
				.build(), HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.CALL_BACK_PATH));
	}
	
	public APIResponseMessage syncExceptionProcess(Throwable e, HttpHeaders headers) {
		log.error("Handler error -> ", e);
		return APIResponseMessage.builder()
				.interfaceId(HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.INTERFACE_ID))
				.transactionId(HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.TRANSACTION_ID))
				.statusCode(ESBStatusConstants.FAIL).statusMessage(e.getMessage())
				.totalDataCount(Integer.valueOf(HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.TOTAL_COUNT)))
				.errorDataCount(Integer.valueOf(HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.TOTAL_COUNT)))
				.build();
	}
}
