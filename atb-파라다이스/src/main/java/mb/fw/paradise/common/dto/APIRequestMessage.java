package mb.fw.paradise.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class APIRequestMessage {

	private String interfaceId;
	
	private String transactionId;
	
	private DataItem dataItem;
	
	private int dataCount;
	
}

