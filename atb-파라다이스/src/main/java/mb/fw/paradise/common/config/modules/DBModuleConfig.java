package mb.fw.paradise.common.config.modules;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import mb.fw.paradise.common.constants.ModuleConfigConstants;

@Data
@Configuration
@ComponentScan(basePackages = ModuleConfigConstants.DB_PACKAGE)
@EnableConfigurationProperties(DBModuleConfig.MybatisProp.class) // 내부 프로퍼티 클래스 활성화
@ConfigurationProperties(prefix = ModuleConfigConstants.DB_PREFIX, ignoreUnknownFields = true)
@ConditionalOnProperty(prefix = ModuleConfigConstants.DB_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = false)
public class DBModuleConfig {

	@Bean("simpleSqlSessionTemplate")
	SqlSessionTemplate simpleSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
		return new SqlSessionTemplate(sqlSessionFactory, ExecutorType.SIMPLE);
	}

	@Bean("batchSqlSessionTemplate")
	SqlSessionTemplate batchSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
		return new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
	}

	@Data
	@ConfigurationProperties(prefix = ModuleConfigConstants.DB_PREFIX + ".mybatis")
	public static class MybatisProp {
		private int thresholdCount = 1000;
		private int batchSize = 1000;
	}
}
