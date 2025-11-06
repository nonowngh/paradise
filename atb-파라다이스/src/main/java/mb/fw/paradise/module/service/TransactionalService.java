package mb.fw.paradise.module.service;

import java.util.ArrayList;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.config.MyBatisConfig;
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

	private final ReceiveQueryExecutor receiveQueryExecutor;
	private final SqlSessionTemplate batchSqlSessionTemplate;
	private final SqlSessionTemplate simpleSqlSessionTemplate;
	private final MyBatisConfig config;

	public TransactionalService(ReceiveQueryExecutor receiveQueryExecutor, MyBatisConfig config,
			@Qualifier("batchSqlSessionTemplate") SqlSessionTemplate batchSqlSessionTemplate,
			@Qualifier("simpleSqlSessionTemplate") SqlSessionTemplate simpleSqlSessionTemplate) {
		this.receiveQueryExecutor = receiveQueryExecutor;
		this.simpleSqlSessionTemplate = simpleSqlSessionTemplate;
		this.config = config;
		this.batchSqlSessionTemplate = batchSqlSessionTemplate;
	}

	@Transactional(rollbackFor = Exception.class)
	public APIResponseMessage transactionalProcess(InterfaceInfo interfaceInfo, APIRequestMessage request)
			throws Exception {
		String workType = InterfaceInfoPropertyUtil.getValue(new ArrayList<>(interfaceInfo.getPropertyList()),
				InterfaceInfoPropertyConstants.DB_WORK_TYPE);
		int dataCount = request.getDataCount();
		SqlSessionTemplate sessionTemplate = dataCount >= config.getThresholdCount() ? batchSqlSessionTemplate
				: simpleSqlSessionTemplate;
		APIResponseMessage response = APIResponseMessage.builder().statusCode(ESBStatusConstants.SUCCESS)
				.statusMessage("처리완료").interfaceId(request.getInterfaceId()).transactionId(request.getTransactionId())
				.dataCount(dataCount).build();
		for (char ch : workType.toCharArray()) {
			switch (ch) {
			case 'D':
				receiveQueryExecutor.processDelete(interfaceInfo, request, sessionTemplate);
				break;
			case 'I':
				if (sessionTemplate == batchSqlSessionTemplate) {
					receiveQueryExecutor.processBatchInsert(interfaceInfo, request, sessionTemplate, config.getBatchSize());
				} else {
					receiveQueryExecutor.processInsert(interfaceInfo, request, sessionTemplate);
				}
				break;
			case 'S':
				DataItem resultItem = receiveQueryExecutor.processSelect(interfaceInfo, request,
						simpleSqlSessionTemplate);
				response.setResultItem(resultItem);
				response.setDataCount(DataItemUtil.tableDataCount(resultItem));
				break;
			default:
				throw new IllegalArgumentException("Invalid 'workType': " + ch);
			}
		}
		return response;
	}

}
