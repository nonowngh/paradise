package mb.fw.paradise.service;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;

import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.dto.APIReqeustMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import reactor.core.publisher.Mono;

@Service
public class ReceiveDBModuleService {

	private final SqlSessionTemplate sqlSessionTemplate;

	public ReceiveDBModuleService(SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}

	public Mono<APIResponseMessage> dbProcessAndResponse(InterfaceInfo interfaceInfo, APIReqeustMessage request) {

		// insert 처리 후 APIResponseMessage return
		return null;
	}
}
