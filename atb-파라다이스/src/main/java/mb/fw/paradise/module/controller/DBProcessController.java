package mb.fw.paradise.module.controller;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mb.fw.paradise.constants.TargetContextPathConstants;
import mb.fw.paradise.dto.APIReqeustMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.module.service.ReceiveDBModuleService;
import mb.fw.paradise.service.APIService;
import reactor.core.publisher.Mono;

@ConditionalOnBean(DataSource.class)
@RestController
@RequestMapping(TargetContextPathConstants.DEFAULT_PATH)
public class DBProcessController {

	private final ReceiveDBModuleService receiveDBModuleService;
	private final APIService apiService;

	public DBProcessController(APIService apiService, ReceiveDBModuleService receiveDBModuleService) {
		this.receiveDBModuleService = receiveDBModuleService;
		this.apiService = apiService;
	}

	@PostMapping(TargetContextPathConstants.RCV_DB_PROCESS)
	public Mono<APIResponseMessage> getInterfaceScheduleList(@RequestBody APIReqeustMessage request) {
		return receiveDBModuleService.dbProcessAndResponse(apiService.getInterfaceInfo(request.getInterfaceId()), request);
	}
}
