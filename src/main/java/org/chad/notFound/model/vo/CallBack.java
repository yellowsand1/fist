package org.chad.notFound.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.chad.notFound.model.RollBackSql;

import java.util.List;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.model.vo
 * @Author: hyl
 * @CreateTime: 2023-04-10  14:37
 * @Description: callback from fist server,only when to rollback
 * @Version: 1.0
 */
@Data
@Accessors(chain = true)
public class CallBack {
    private String fistId;
    @JsonProperty("rollbackSql")
    private List<RollBackSql> rollBackSql;
    private String group;
}
