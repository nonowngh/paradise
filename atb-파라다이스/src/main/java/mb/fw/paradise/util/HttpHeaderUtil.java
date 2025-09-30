package mb.fw.paradise.util;

import org.springframework.http.HttpHeaders;

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
}
