package org.chad.notFound.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.model
 * @Author: hyl
 * @CreateTime: 2023-04-04  11:19
 * @Description: To make rust server recognize this distributed transaction
 * and know where to call back
 * @Version: 1.0
 */
@Data
@Accessors(chain = true)
public class Identifier {
    private String traceId;
}
