package org.chad.notFound.model;

import lombok.Data;
import lombok.experimental.Accessors;
import org.chad.notFound.configuration.FistProperties;
import org.chad.notFound.configuration.FistSpringContextHolder;

import java.io.Serializable;
import java.util.List;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.model
 * @Author: hyl
 * @CreateTime: 2023-04-04  11:01
 * @Description: sync every distributed transaction,
 * contains no sql but only distribute identifier
 * and rollback or not
 * @Version: 1.0
 */
@Data
@Accessors(chain = true)
public class SyncInfo implements Serializable {
    private static final long serialVersionUID = 1136632001612302881L;
    private String fistId;
    private boolean rollback;
    private List<RollBackSql> rollbackSql;
    private boolean end;
    private String group;
    private Integer servicePort;

    public SyncInfo() {
        this.servicePort = FistSpringContextHolder.getBean(FistProperties.class).getPort();
    }
}