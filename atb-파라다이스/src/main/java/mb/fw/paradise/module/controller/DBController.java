package mb.fw.paradise.module.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mb.fw.paradise.constants.TargetModuleContextPathConstants;
import mb.fw.paradise.dto.APIReqeustMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.service.APIService;
import mb.fw.paradise.service.ReceiveDBModuleService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(TargetModuleContextPathConstants.DEFAULT_PATH)
public class DBController {

	private final ReceiveDBModuleService receiveDBModuleService;
	private final APIService apiService;

	public DBController(APIService apiService, ReceiveDBModuleService receiveDBModuleService) {
		this.receiveDBModuleService = receiveDBModuleService;
		this.apiService = apiService;
	}

	@PostMapping(TargetModuleContextPathConstants.RCV_DB_PROCESS)
	public Mono<APIResponseMessage> getInterfaceScheduleList(@RequestBody APIReqeustMessage request) {
		return receiveDBModuleService.dbProcessAndResponse(apiService.getInterfaceInfo(request.getInterfaceId()), request);
	}
}
