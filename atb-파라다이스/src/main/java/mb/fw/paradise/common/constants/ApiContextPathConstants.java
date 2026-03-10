package mb.fw.paradise.common.constants;

public class ApiContextPathConstants {

	private ApiContextPathConstants() {
	}

	// 기본 root path
	public static final String DEFAULT_PATH = "/esb/api";

	// 인터페이스 정보 API path
	public static final String META_API = DEFAULT_PATH + "/meta";

	// 인터페이스 정보 API clear cache path
	public static final String META_API_CLEAR_CACHE = "/clear-cache";

	// Gateway path
	public static final String GATEWAY = DEFAULT_PATH + "/gateway";
	
	// 인터페이스 스케줄 정보
	public static final String META_API_SCHEDULE_LIST = "/schedule-list";

	// RFC FUNCTION 명에 해당하는 인터페이스 정보
	public static final String META_API_LIST_RFC_FUNCTION = "/meta-list-rfc-function";
}
