package mb.fw.paradise.module.service;

import java.util.ArrayList;

import org.springframework.stereotype.Service;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoFunction;

import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorType;
import mb.fw.paradise.constants.ESBStatusConstants;
import mb.fw.paradise.constants.InterfaceInfoPropertyConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.util.InterfaceInfoPropertyUtil;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@ConditionalOnAdaptorType(AdaptorType.RFC)
public class RFCModuleService {

	private final JCoDestination jcoDestination;

	public RFCModuleService(JCoDestination jcoDestination) {
		this.jcoDestination = jcoDestination;
	}

	public Mono<APIResponseMessage> importTables(InterfaceInfo interfaceInfo, APIRequestMessage request) {
		return Mono.fromCallable(() -> {
			String functionName = InterfaceInfoPropertyUtil.getValue(new ArrayList<>(interfaceInfo.getPropertyList()),
					InterfaceInfoPropertyConstants.RFC_FUNCTION_NAME);
			JCoFunction function = jcoDestination.getRepository().getFunction(functionName);
			if (function == null)
				throw new RuntimeException("RFC Function not found! -> " + functionName);

			return APIResponseMessage.builder().statusCode(ESBStatusConstants.SUCCESS)
					.interfaceId(request.getInterfaceId()).transactionId(request.getTransactionId())
					.totalDataCount(request.getTotalDataCount()).build();
		}).subscribeOn(Schedulers.boundedElastic()) // 블로킹 작업 안전 처리
				.onErrorResume(e -> {
					return Mono.error(new RuntimeException("RFC 처리 실패: " + e.getMessage(), e));
				});

	}
}
