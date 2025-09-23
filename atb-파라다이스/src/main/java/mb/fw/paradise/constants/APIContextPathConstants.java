package mb.fw.paradise.constants;

public class APIContextPathConstants {

	private APIContextPathConstants() {
	}

	// 기본 root path
	public static final String DEFAULT_PATH = "/esb/api";

	// 인터페이스 정보 API path
	public static final String INTERFACE_INFO_API = DEFAULT_PATH + "/interface_info";

	// 인터페이스 정보 API clear cache path
	public static final String INTERFACE_INFO_API_CLEAR_CACHE = DEFAULT_PATH + "/interface_info/clear-cache";

	// Gateway path
	public static final String GATEWAY = DEFAULT_PATH + "/gateway";
}
