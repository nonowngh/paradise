package mb.fw.paradise.constants;

import java.util.Arrays;

import lombok.Getter;

public enum PatternType {

	// DB2RFC
	DB_RFC("D2R", TargetContextPathConstants.RCV_RFC_CALL),
	// API2RFC
	API_RFC("A2R", TargetContextPathConstants.RCV_RFC_CALL),
	// RFC2DB(배치)
	RFCBATCH_DB("RB2B", TargetContextPathConstants.RCV_DB_PROCESS),
	// RFC2DB(서버)
	RFCSERVER_DB("RS2B", TargetContextPathConstants.RCV_DB_PROCESS),
	// RFC2API(서버)
	RFCSERVER_API("RS2A", TargetContextPathConstants.RCV_API_CALL),
	// DB2DB
	DB_DB("D2D", TargetContextPathConstants.RCV_DB_PROCESS),
	// API2DB
	API_DB("A2D", TargetContextPathConstants.RCV_DB_PROCESS),
	// API(DB)2DB
	APIDB_DB("AD2D", TargetContextPathConstants.RCV_DB_PROCESS),
	// API(DB)2RFC
	APIDB_RFC("AD2R", TargetContextPathConstants.RCV_RFC_CALL);

	@Getter
	private final String code;
	@Getter
	private final String targetContextPath;

	PatternType(String code, String targetContextPath) {
		this.code = code;
		this.targetContextPath = targetContextPath;
	}

	public static PatternType fromPatternType(String code) {
		return Arrays.stream(PatternType.values()).filter(value -> value.getCode().equals(code))
				.findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid pattern-type code : " + code));
	}
}
