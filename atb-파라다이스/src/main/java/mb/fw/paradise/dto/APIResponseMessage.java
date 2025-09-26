package mb.fw.paradise.dto;

import lombok.Data;

@Data
public class APIResponseMessage {

	private String interfaceId;
	
	private String transactionId;
	
	private DataItem dataItem;
	
	private String statusCode;
	
	private String statusMessage;
	
}

