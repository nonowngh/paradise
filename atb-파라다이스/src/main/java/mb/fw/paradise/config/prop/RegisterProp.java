package mb.fw.paradise.config.prop;

import java.util.List;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class RegisterProp {

	// 배치 스케줄 실행 task 클래스
	private String batchTask;
	// 해당 어댑터 등록 인터페이스 리스트
	private List<String> interfaceList;
	// 해당 어댑터 시스템 코드
	private String systemCode;

}
