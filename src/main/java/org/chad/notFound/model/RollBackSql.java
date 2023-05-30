package org.chad.notFound.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.model
 * @Author: hyl
 * @CreateTime: 2023-04-06  17:17
 * @Description: rollback info used to generate rollback preparedStatement
 * @Version: 1.0
 */
@Data
@Accessors(chain = true)
public class RollBackSql implements Serializable {
    private static final long serialVersionUID = -8136575850163855224L;
    private String sql;
    private List<List<Object>> params;
}
