package mb.fw.paradise.constants;

import java.util.Arrays;

import lombok.Getter;

public enum TargetModule {
	
	// DB 수신 처리 패턴
	RCV_DB_PROCESS("DB_PROCESS","/rcv-db-process"),
	// RFC 수신 CALL 처리 패턴
	RCV_RFC_CALL("RFC_CALL","/rcv-rfc-call");
	
	@Getter
	private final String patternType;
	@Getter
	private final String contextPath;

	TargetModule(String patternType, String contextPath) {
		this.patternType = patternType;
		this.contextPath = contextPath;
	}
	
	public static TargetModule fromPatternType(String patternType) {
		return Arrays.stream(TargetModule.values()).filter(value -> value.getPatternType().equals(patternType)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Invalid pattern-type : " + patternType));
	}
}
