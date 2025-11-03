package mb.fw.paradise.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerResponse;

import mb.fw.paradise.constants.ESBApiHeaderConstants;
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
	
	public static int getIntHeaderIgnoreCase(HttpHeaders headers, String key) {
	    String value = getHeaderIgnoreCase(headers, key);
	    try {
	        return Integer.parseInt(value);
	    } catch (NumberFormatException e) {
	        return 0;
	    }
	}
	
	public static String getHeaderIgnoreCase(HttpHeaders headers, String key) {
	    for (String headerName : headers.keySet()) {
	        if (headerName.equalsIgnoreCase(key)) {
	            return headers.getFirst(headerName);
	        }
	    }
	    return ""; // 없으면 빈 문자열 반환
	}

	public static ServerResponse.BodyBuilder makeDefaultOkResponseHeader(HttpHeaders requestHeader) {
		Set<String> allowedHeaders = new HashSet<>(
				Arrays.asList(ESBApiHeaderConstants.INTERFACE_ID, ESBApiHeaderConstants.TRANSACTION_ID,
						ESBApiHeaderConstants.SEND_SYSTEM_CODE, ESBApiHeaderConstants.RECEIVE_SYSTEM_CODE,
						ESBApiHeaderConstants.DATA_COUNT, ESBApiHeaderConstants.API_MESSAGE_TYPE));

		ServerResponse.BodyBuilder responseBuilder = ServerResponse.ok();
		requestHeader.forEach((key, values) -> {
			if (allowedHeaders.contains(key)) {
				values.forEach(value -> responseBuilder.header(key, value));
			}
		});
		responseBuilder.header(ESBApiHeaderConstants.ESB_STATUS_CODE, ESBStatusConstants.SUCCESS);
		responseBuilder.header(ESBApiHeaderConstants.ESB_STATUS_MESSAGE, Base64.getEncoder().encodeToString("처리 완료".getBytes(StandardCharsets.UTF_8)));
		return responseBuilder;
	}

	public static ServerResponse.BodyBuilder makeDefaultErrorResponseHeader(HttpHeaders requestHeader,
			String statusMessage) {
		Set<String> allowedHeaders = new HashSet<>(
				Arrays.asList(ESBApiHeaderConstants.INTERFACE_ID, ESBApiHeaderConstants.TRANSACTION_ID,
						ESBApiHeaderConstants.SEND_SYSTEM_CODE, ESBApiHeaderConstants.RECEIVE_SYSTEM_CODE,
						ESBApiHeaderConstants.DATA_COUNT, ESBApiHeaderConstants.API_MESSAGE_TYPE));

		ServerResponse.BodyBuilder responseBuilder = ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR);
		requestHeader.forEach((key, values) -> {
			if (allowedHeaders.contains(key)) {
				values.forEach(value -> responseBuilder.header(key, value));
			}
		});
		responseBuilder.header(ESBApiHeaderConstants.ESB_STATUS_CODE, ESBStatusConstants.FAIL);
		responseBuilder.header(ESBApiHeaderConstants.ESB_STATUS_MESSAGE, Base64.getEncoder().encodeToString(statusMessage.getBytes(StandardCharsets.UTF_8)));
		return responseBuilder;
	}
	
	public static boolean isBase64(String str) {
	    if (str == null || str.isEmpty()) {
	        return false;
	    }

	    try {
	        // 실제로 디코딩 시도
	        Base64.getDecoder().decode(str);
	        return true;
	    } catch (IllegalArgumentException e) {
	        // 디코딩 실패 → Base64 아님
	        return false;
	    }
	}
}
