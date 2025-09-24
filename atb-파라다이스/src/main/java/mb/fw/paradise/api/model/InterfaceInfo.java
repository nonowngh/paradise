package mb.fw.paradise.api.model;

import lombok.Data;

@Data
public class InterfaceInfo {

	private String interfaceId;
	
	private String cronExpression;
	
	private String targetPatternType;
	
}
