package mb.fw.paradise.module.metaapi.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 복합키 식별자 클래스
@Data
@NoArgsConstructor
@AllArgsConstructor
class InterfaceDetailId implements Serializable {
    private String interfaceId;
    private String propertyName;
}

@Entity
@Table(name = "TB_INTERFACE_DETAIL")
@IdClass(InterfaceDetailId.class)
@Data
@NoArgsConstructor
public class MetaDetailEntity {

    @Id
    @Column(name = "INTERFACE_ID", length = 30)
    private String interfaceId;

    @Id
    @Column(name = "PROPERTY_NAME", length = 100)
    private String propertyName;

    @Column(name = "PROPERTY_VALUE", length = 1000)
    private String propertyValue;

    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;

    @Column(name = "UPDATED_BY", length = 50)
    private String updatedBy;
    
    // CreatedAt/UpdatedAt은 부모와 동일하게 @CreationTimestamp 적용 가능
}