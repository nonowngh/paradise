package mb.fw.paradise.module.metaapi.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class InterfaceQueryId implements Serializable {
    private String interfaceId;
    private String sqlId;
}

@Entity
@Table(name = "TB_INTERFACE_QUERY")
@IdClass(InterfaceQueryId.class)
@Data
@NoArgsConstructor
public class MetaQueryEntity {

    @Id
    @Column(name = "INTERFACE_ID", length = 30)
    private String interfaceId;

    @Id
    @Column(name = "SQL_ID", length = 50)
    private String sqlId;

    @Lob // CLOB 타입 매핑
    @Column(name = "QUERY", nullable = false)
    private String query;

    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;

    @Column(name = "UPDATED_BY", length = 50)
    private String updatedBy;
}