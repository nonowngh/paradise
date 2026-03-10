package mb.fw.paradise.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class TransactionGeneratorUtil {
    private static final AtomicInteger seq = new AtomicInteger(0);

    public static String getNextSequence() {
        int current = seq.getAndUpdate(n -> (n >= 999) ? 0 : n + 1);
        return String.format("%03d", current);
    }
    
    public static String getDateTimeNow() {
    	return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}