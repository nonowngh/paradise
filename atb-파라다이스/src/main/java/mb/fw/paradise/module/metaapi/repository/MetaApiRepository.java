package mb.fw.paradise.module.metaapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import mb.fw.paradise.module.metaapi.entity.MetaInfoEntity;

@Repository
public interface MetaApiRepository extends JpaRepository<MetaInfoEntity, String> {

	/**
	 * [MyBatis: selectInterfaceWithDetails] Fetch Join을 사용하여 Detail과 Query 목록을 한 번의
	 * 쿼리로 가져옵니다. DISTINCT는 1:N 조인 시 결과 row가 뻥튀기되는 것을 방지합니다.
	 */
	@Query("SELECT DISTINCT i FROM MetaInfoEntity i " + "LEFT JOIN FETCH i.propertyList "
			+ "LEFT JOIN FETCH i.sqlQueryList " + "WHERE i.interfaceId = :interfaceId AND i.useYn = 'Y'")
	Optional<MetaInfoEntity> findWithDetailsByInterfaceId(@Param("interfaceId") String interfaceId);

	/**
	 * [MyBatis: selectInterfaceCronExpressionList]
	 */
	List<MetaInfoEntity> findByInterfaceIdIn(List<String> interfaceIdList);

	/**
	 * [MyBatis: selectInterfaceListWithFunctionName] 서브쿼리(EXISTS)를 사용하여 특정 기능을 가진
	 * 인터페이스 리스트를 조회합니다.
	 */
	@Query("SELECT DISTINCT i FROM MetaInfoEntity i " + "LEFT JOIN FETCH i.propertyList "
			+ "LEFT JOIN FETCH i.sqlQueryList " + "WHERE i.useYn = 'Y' " + "AND i.interfaceId IN :interfaceIdList "
			+ "AND EXISTS (SELECT 1 FROM InterfaceDetailEntity d " + "            WHERE d.interfaceId = i.interfaceId "
			+ "            AND d.propertyName = 'RFC_FUNCTION_NAME' "
			+ "            AND d.propertyValue = :functionName)")
	List<MetaInfoEntity> findWithDetailsByFunctionName(@Param("interfaceIdList") List<String> interfaceIdList,
			@Param("functionName") String functionName);

	// 필요한 컬럼만 딱 집어서 가져오기 (성능 최적화)
	@Query("SELECT i.interfaceId as interfaceId, i.cronExpression as cronExpression "
			+ "FROM MetaApiEntity i WHERE i.useYn = 'Y'")
	List<InterfaceCronOnly> findAllCronExpressions();
}