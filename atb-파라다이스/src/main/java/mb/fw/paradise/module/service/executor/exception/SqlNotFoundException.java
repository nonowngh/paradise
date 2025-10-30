package mb.fw.paradise.module.service.executor.exception;

public class SqlNotFoundException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SqlNotFoundException(String sqlId) {
        super("❌ SQL not found for sqlId: " + sqlId);
    }
}
