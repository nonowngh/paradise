package mb.fw.paradise.util;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.server.ServerResponse;

import mb.fw.paradise.constants.ESBAPIHeaderConstants;
import mb.fw.paradise.constants.ESBStatusConstants;

public class HttpHeaderUtil {
	public static String getHeader(HttpHeaders headers, String key) {
		String value = headers.getFirst(key);
		return value != null ? value : "";
	}

	public static int getIntHeader(HttpHeaders headers, String key) {
		try {
			String value = headers.getFirst(key);
			return value != null ? Integer.parseInt(value) : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public static ServerResponse.BodyBuilder makeDefaultResponseHeader(HttpHeaders requestHeader, String statusCode,
			String statusMessage) {
		ServerResponse.BodyBuilder responseBuilder = ServerResponse.ok();
		requestHeader.forEach((key, values) -> {
			for (String value : values) {
				responseBuilder.header(key, value);
			}
		});
		responseBuilder.header(ESBAPIHeaderConstants.ESB_STATUS_CODE, statusCode);
		responseBuilder.header(ESBAPIHeaderConstants.ESB_STATUS_MESSAGE, statusMessage);
		responseBuilder.header(ESBAPIHeaderConstants.ERROR_COUNT,
				statusCode == ESBStatusConstants.SUCCESS ? String.valueOf(0)
						: getHeader(requestHeader, ESBAPIHeaderConstants.TOTAL_COUNT));
		return responseBuilder;
	}
}
