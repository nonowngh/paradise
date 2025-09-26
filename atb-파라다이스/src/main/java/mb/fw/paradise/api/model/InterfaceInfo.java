package mb.fw.paradise.api.model;

import java.util.List;

import lombok.Data;

@Data
public class InterfaceInfo {

	// 공통 항목
	private String interfaceId;
	
	private String cronExpression;
	
	private String patternCode;
	
	private String sndSystemCode;
	
	private String rcvSystemCode;
	
	// DB 패턴	
	private String sndTableNames;
	
	private String rcvTableNames;
	
	private List<SqlQuery> sqlQueryList;
	
	private String workType;
	
}
