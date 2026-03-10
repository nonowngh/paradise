package mb.fw.paradise.module.db.service;

import java.util.ArrayList;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mb.fw.paradise.common.config.modules.DBModuleConfig.MybatisProp;
import mb.fw.paradise.common.constants.ESBStatusConstants;
import mb.fw.paradise.common.constants.InterfaceInfoPropertyConstants;
import mb.fw.paradise.common.dto.APIRequestMessage;
import mb.fw.paradise.common.dto.APIResponseMessage;
import mb.fw.paradise.common.dto.DataItem;
import mb.fw.paradise.common.util.DataItemUtil;
import mb.fw.paradise.common.util.InterfaceInfoPropertyUtil;
import mb.fw.paradise.module.db.service.executor.ReceiveQueryExecutor;
import mb.fw.paradise.module.metaapi.model.MetaApiModel;

@Service
public class TransactionalService {

	private final ReceiveQueryExecutor receiveQueryExecutor;
	private final SqlSessionTemplate batchSqlSessionTemplate;
	private final SqlSessionTemplate simpleSqlSessionTemplate;
	private final MybatisProp property;

	public TransactionalService(ReceiveQueryExecutor receiveQueryExecutor, MybatisProp property,
			@Qualifier("batchSqlSessionTemplate") SqlSessionTemplate batchSqlSessionTemplate,
			@Qualifier("simpleSqlSessionTemplate") SqlSessionTemplate simpleSqlSessionTemplate) {
		this.receiveQueryExecutor = receiveQueryExecutor;
		this.simpleSqlSessionTemplate = simpleSqlSessionTemplate;
		this.property = property;
		this.batchSqlSessionTemplate = batchSqlSessionTemplate;
	}

	@Transactional(rollbackFor = Exception.class)
	public APIResponseMessage transactionalProcess(MetaApiModel interfaceInfo, APIRequestMessage request)
			throws Exception {
		String workType = InterfaceInfoPropertyUtil.getValue(new ArrayList<>(interfaceInfo.getPropertyList()),
				InterfaceInfoPropertyConstants.DB_WORK_TYPE);
		int dataCount = request.getDataCount();
		SqlSessionTemplate sessionTemplate = dataCount >= property.getThresholdCount() ? batchSqlSessionTemplate
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
					receiveQueryExecutor.processBatchInsert(interfaceInfo, request, sessionTemplate, property.getBatchSize());
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
