package mb.fw.paradise.module.send;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.constants.TargetModule;
import mb.fw.paradise.dto.APIReqeustMessage;
import mb.fw.paradise.dto.DataItem;
import mb.fw.paradise.module.BatchModule;
import mb.fw.paradise.service.APIService;

@Slf4j
@Component("DBPollingAndSend")
public class DBPollingAndSend implements BatchModule {

	private final APIService apiService;

	public DBPollingAndSend(APIService apiService) {
		this.apiService = apiService;
	}

	@Override
	public void executeTask(String interfaceId) {
		log.info("excute -> [{}]", interfaceId);
		InterfaceInfo interfaceInfo = apiService.getInterfaceInfo(interfaceId);
		String targetPatternType = interfaceInfo.getTargetPatternType();

		// db polling..
		DataItem dataItem = new DataItem();

		apiService.callGateway(APIReqeustMessage.builder().interfaceId(interfaceId).dataItem(dataItem).build(),
				TargetModule.fromPatternType(targetPatternType)).subscribe();
	}

}
