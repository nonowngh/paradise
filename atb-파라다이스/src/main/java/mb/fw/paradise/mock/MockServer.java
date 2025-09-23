package mb.fw.paradise.mock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Profile("local")
@Component
@RequiredArgsConstructor
public class MockServer {
	private ClientAndServer mockServer;

	@PostConstruct
	public void startServer() {

		mockServer = ClientAndServer.startClientAndServer(8090);

		mockServer.when(HttpRequest.request().withMethod("POST").withPath("/esb/api/recv-ad1"))
				.respond(HttpResponse.response().withStatusCode(200).withBody(
						"A00000000000000000000000000000000000000000000000030dososo   12345        0101234567802245865845   "));
	}

	@PreDestroy
	public void stopServer() {
		mockServer.stop();
	}
}
