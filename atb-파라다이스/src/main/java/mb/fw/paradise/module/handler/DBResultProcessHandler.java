package mb.fw.paradise.module.handler;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.model.SqlQuery;
import mb.fw.paradise.constants.ESBCommonFieldConstants;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.module.service.SendDBModuleService;
import mb.fw.paradise.service.APIService;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class DBResultProcessHandler {

	private final SendDBModuleService sendDBModuleService;
	private final APIService apiService;

	public DBResultProcessHandler(APIService apiService, SendDBModuleService sendDBModuleService) {
		this.sendDBModuleService = sendDBModuleService;
		this.apiService = apiService;
	}

	public Mono<ServerResponse> dbResultProcess(ServerRequest serverRequest) {
		APIResponseMessage response = (APIResponseMessage) serverRequest.attributes().get("cachedBody");

		if (response == null) {
			return ServerResponse.badRequest().bodyValue("요청 body가 존재하지 않습니다.");
		}

		return apiService.getInterfaceInfo(response.getInterfaceId()) // 인터페이스 정보 조회
				.flatMap(interfaceInfo -> {
					List<String> tableNameList = Arrays.stream(interfaceInfo.getSndTableNames().split(","))
							.map(String::trim).collect(Collectors.toList());
					List<SqlQuery> queryList = interfaceInfo.getSqlQueryList();
					// 필수 파라미터 설정
					Map<String, Object> params = Stream.of(
							new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_IF_ID, response.getInterfaceId()),
							new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_TX_ID, response.getTransactionId()))
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
					log.info("result update count : {}",
							sendDBModuleService.updateResult(tableNameList, queryList, params));
					return Mono.empty();
				}).onErrorMap(error -> {
					log.error("Error [dbResultProcess] -> {}", error.getMessage()); // 에러 처리
					return new RuntimeException(error.getMessage(), error);
				}).then(ServerResponse.ok().bodyValue("[dbResultProcess] 요청 수신 완료."));
	}

}
