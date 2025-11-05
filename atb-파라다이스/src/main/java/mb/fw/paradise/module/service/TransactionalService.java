package mb.fw.paradise.module.service;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorType;
import mb.fw.paradise.constants.ESBStatusConstants;
import mb.fw.paradise.constants.InterfaceInfoPropertyConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.dto.DataItem;
import mb.fw.paradise.module.service.executor.ReceiveQueryExecutor;
import mb.fw.paradise.util.DataItemUtil;
import mb.fw.paradise.util.InterfaceInfoPropertyUtil;

@Service
@ConditionalOnAdaptorType(AdaptorType.DB)
public class TransactionalService {

	@Autowired
	private ReceiveQueryExecutor receiveQueryExecutor;

	@Transactional(rollbackFor = Exception.class)
	public APIResponseMessage transactionalProcess(InterfaceInfo interfaceInfo, APIRequestMessage request)
			throws Exception {
		String workType = InterfaceInfoPropertyUtil.getValue(new ArrayList<>(interfaceInfo.getPropertyList()),
				InterfaceInfoPropertyConstants.DB_WORK_TYPE);
		APIResponseMessage response = APIResponseMessage.builder().statusCode(ESBStatusConstants.SUCCESS)
				.statusMessage("처리완료").interfaceId(request.getInterfaceId()).transactionId(request.getTransactionId())
				.dataCount(request.getDataCount()).build();
		for (char ch : workType.toCharArray()) {
			switch (ch) {
			case 'D':
				receiveQueryExecutor.processDelete(interfaceInfo, request);
				break;
			case 'I':
				receiveQueryExecutor.processInsert(interfaceInfo, request);
				break;
//				case 'U':
//					receiveQueryExecutor.processUpdateQueries(interfaceInfo, request);
//					break;
//				case 'P':
//					receiveQueryExecutor.processProcedureQueries(interfaceInfo, request);
//					break;
			case 'S':
				DataItem resultItem = receiveQueryExecutor.processSelect(interfaceInfo, request);
				response.setResultItem(resultItem);
				response.setDataCount(DataItemUtil.tableDataCount(resultItem));
				break;
			default:
				throw new IllegalArgumentException("Invalid 'workType' : " + ch);
			}
		}
		return response;
	}

}
