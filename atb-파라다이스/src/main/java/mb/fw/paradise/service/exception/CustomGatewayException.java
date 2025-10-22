package mb.fw.paradise.service.exception;

import mb.fw.paradise.dto.APIResponseMessage;

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