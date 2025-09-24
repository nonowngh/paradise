package mb.fw.paradise.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class APIReqeustMessage {

	private String interfaceId;
	
	private String transactionId;
	
	private DataItem dataItem;
	
	private String callBackPath;
	
}

