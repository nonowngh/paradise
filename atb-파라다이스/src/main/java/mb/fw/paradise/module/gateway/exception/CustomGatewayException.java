package mb.fw.paradise.module.gateway.exception;

import mb.fw.paradise.common.dto.APIResponseMessage;

public class CustomGatewayException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final APIResponseMessage apiResponse;

    public CustomGatewayException(String message, APIResponseMessage response) {
        super(message);
        this.apiResponse = response;
    }

    public APIResponseMessage getApiResponse() {
        return apiResponse;
    }
}