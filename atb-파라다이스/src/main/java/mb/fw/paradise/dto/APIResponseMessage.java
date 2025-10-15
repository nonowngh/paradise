package mb.fw.paradise.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class APIResponseMessage {

	private String interfaceId;

	private String transactionId;

	private DataItem resultItem;

	private String statusCode;

	private String statusMessage;

	private int errorDataCount;

	private int totalDataCount;

}
