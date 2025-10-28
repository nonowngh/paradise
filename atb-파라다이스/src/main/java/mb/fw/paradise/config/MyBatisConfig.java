package mb.fw.paradise.config;


import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import lombok.Getter;
import lombok.Setter;
import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorType;

//@Configuration
@ConditionalOnAdaptorType(AdaptorType.DB)
@ConfigurationProperties(prefix = "mybatis.batch", ignoreUnknownFields = true)
public class MyBatisConfig {

	@Setter@Getter
	private int thresholdCount = 1000;

    @Bean("simpleSqlSessionTemplate")
    SqlSessionTemplate simpleSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory, ExecutorType.SIMPLE);
    }

    @Bean("batchSqlSessionTemplate")
    SqlSessionTemplate batchSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
    }
}
