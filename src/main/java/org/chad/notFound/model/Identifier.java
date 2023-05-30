package org.chad.notFound.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

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
public class Identifier implements Serializable {
    private static final long serialVersionUID = 705571351668004486L;
    private String traceId;
}
