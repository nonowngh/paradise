package mb.fw.paradise.module.metaapi.entity;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TB_INTERFACE_INFO")
@Data
@NoArgsConstructor
public class MetaInfoEntity {

	@Id
	@Column(name = "INTERFACE_ID", length = 30)
	private String interfaceId;

	@Column(name = "CRON_EXPRESSION", length = 30, nullable = false)
	private String cronExpression;

	@Column(name = "PATTERN_TYPE", length = 10, nullable = false)
	private String patternType;

	@Column(name = "SEND_SYSTEM_CODE", length = 3, nullable = false)
	private String sendSystemCode;

	@Column(name = "RECV_SYSTEM_CODE", length = 3, nullable = false)
	private String recvSystemCode;

	@Column(name = "MAPPING_YN", length = 1, nullable = false)
	private String mappingYn = "N";

	@Column(name = "USE_YN", length = 1, nullable = false)
	private String useYn = "Y";

	@CreationTimestamp
	@Column(name = "CREATED_AT", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "UPDATED_AT")
	private LocalDateTime updatedAt;

	@Column(name = "CREATED_BY", length = 50)
	private String createdBy;

	@Column(name = "UPDATED_BY", length = 50)
	private String updatedBy;

	// 상세 속성 (1:N)
	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "INTERFACE_ID")
	private Set<MetaDetailEntity> propertyList = new LinkedHashSet<>();

	// 쿼리 정보 (1:N)
	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "INTERFACE_ID")
	private Set<MetaQueryEntity> sqlQueryList = new LinkedHashSet<>();
}