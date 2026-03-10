package mb.fw.paradise.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class APIResponseMessage {

	private String interfaceId;

	private String transactionId;

	private DataItem resultItem;

	private String statusCode;

	private String statusMessage;

	private int dataCount;

}
