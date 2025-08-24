package inha.gdgoc.global.config.openapi;

import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi all() {
        return groupedApi("all", "/**");
    }

    @Bean
    public GroupedOpenApi v1Api() {
        return groupedApi("v1", "/api/v1");
    }

     @Bean
     public GroupedOpenApi v2Api() {
       return groupedApi("v2", "/api/v2");
     }

    private GroupedOpenApi groupedApi(String group, String fullPrefix) {
        return GroupedOpenApi.builder()
            .group(group)
            .pathsToMatch(fullPrefix + "/**")
            .addOpenApiCustomizer(stripPrefixAndSetServer(fullPrefix))
            .build();
    }

    private OpenApiCustomizer stripPrefixAndSetServer(String fullPrefix) {
        return openApi -> {
            Paths src = openApi.getPaths();
            if (src == null || src.isEmpty()) return;

            Paths dst = new Paths();
            src.forEach((path, item) -> {
                String p = path;
                if (p.equals(fullPrefix)) p = "/";
                else if (p.startsWith(fullPrefix + "/")) p = p.substring(fullPrefix.length());
                dst.addPathItem(p, item);
            });

            openApi.setPaths(dst);
            openApi.setServers(List.of(new Server().url(fullPrefix)));
        };
    }
}
