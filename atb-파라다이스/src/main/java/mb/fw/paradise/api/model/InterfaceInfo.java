package mb.fw.paradise.api.model;

import java.util.LinkedHashSet;
import java.util.Set;

import lombok.Data;

@Data
public class InterfaceInfo {

	// 공통 항목
	private String interfaceId;

	private String cronExpression;

	private String patternType;

	private String sndSystemCode;

	private String rcvSystemCode;

	private Set<PatternProperty> propertyList = new LinkedHashSet<>();
	
	private Set<SqlQuery> sqlQueryList = new LinkedHashSet<>();

	// // DB 패턴
//	private String sndTableNames;
//	
//	private String rcvTableNames;
//	
//	
//	private String workType;

}
