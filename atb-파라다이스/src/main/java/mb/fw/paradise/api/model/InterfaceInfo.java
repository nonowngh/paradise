package mb.fw.paradise.api.model;

import java.util.List;

import lombok.Data;

@Data
public class InterfaceInfo {

	private String interfaceId;
	
	private String cronExpression;
	
	private String targetPatternType;
	
	private String sndTableNames;
	
	private String rcvTableNames;
	
	private List<SqlQuery> sqlQueryList;
	
}
