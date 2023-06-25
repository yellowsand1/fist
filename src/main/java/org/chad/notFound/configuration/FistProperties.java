package org.chad.notFound.configuration;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.configuration
 * @Author: hyl
 * @CreateTime: 2023-04-01  15:29
 * @Description: web properties for fist
 * @Version: 1.0
 */
@Getter
public class FistProperties {
    @Value("${fist.server.addr:127.0.0.1}")
    private String fistServerAddr;

    @Value("${fist.server.port:18511}")
    private Integer fistServerPort;

    @Value("${fist.target.database.url}")
    private String fistTargetDatabaseUrl;

    @Value("${fist.target.database.username}")
    private String fistTargetDatabaseUsername;

    @Value("${fist.target.database.password}")
    private String fistTargetDatabasePassword;

    @Value("${server.port:8080}")
    private Integer port;
}
