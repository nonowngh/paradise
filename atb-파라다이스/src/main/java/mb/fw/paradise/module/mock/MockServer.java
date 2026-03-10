package mb.fw.paradise.module.mock;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import mb.fw.paradise.common.constants.ApiContextPathConstants;
import mb.fw.paradise.module.metaapi.model.MetaApiModel;

@Profile("local")
@Component
@RequiredArgsConstructor
public class MockServer {
	private ClientAndServer mockServer;

	@PostConstruct
	public void startServer() throws JsonProcessingException {

		mockServer = ClientAndServer.startClientAndServer(8090);
		List<MetaApiModel> infoList = new ArrayList<>();
		MetaApiModel info = new MetaApiModel();
		info.setInterfaceId("IF_UAS_CB_TRS_002");
		info.setCronExpression("0/10 * * * * ?");
		infoList.add(info);
		mockServer.when(HttpRequest.request().withMethod("POST").withPath(ApiContextPathConstants.META_API + ApiContextPathConstants.META_API_SCHEDULE_LIST))
				.respond(HttpResponse.response().withStatusCode(200).withHeader("Content-Type", "application/json").withBody(
						new ObjectMapper().writeValueAsString(infoList)));
	}

	@PreDestroy
	public void stopServer() {
		mockServer.stop();
	}
}
