package inha.gdgoc.global.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /**
     * OpenAPI 메인 빈을 생성합니다.
     *
     * OpenAPI 인스턴스에 API 정보(제목 "GDGoC API", 버전 "v1")를 설정하고,
     * HTTP Bearer JWT 형식의 보안 스킴 "BearerAuth"를 Components에 추가합니다.
     *
     * @return 구성된 OpenAPI 인스턴스
     */
    @Bean
    public OpenAPI openAPI() {
        String schemeName = "BearerAuth";
        return new OpenAPI()
                .info(new Info().title("GDGoC API").version("v1"))
                .components(new Components().addSecuritySchemes(
                        schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                        )
                );
    }

    /**
     * 모든 엔드포인트를 포함하는 GroupedOpenApi 빈을 생성합니다.
     *
     * <p>그룹 이름은 "all"이며, 모든 경로(" /**")를 매칭하도록 구성됩니다.
     *
     * @return 모든 경로를 포함하는 GroupedOpenApi 인스턴스
     */
    @Bean
    public GroupedOpenApi all() {
        return GroupedOpenApi.builder()
                .group("all")
                .pathsToMatch("/**")
                .build();
    }

    /**
     * v1 API 그룹을 위한 GroupedOpenApi 빈을 생성합니다.
     *
     * "v1" 그룹 이름과 "/api/v1" 경로 접두사를 사용하여 groupedApi(...)를 호출하여,
     * 해당 접두사가 제거된 경로와 그룹 전용 Server URL이 설정된 GroupedOpenApi 인스턴스를 반환합니다.
     *
     * @return "/api/v1" 접두사가 적용된 엔드포인트들을 그룹화하고 프리픽스를 제거한 GroupedOpenApi
     */
    @Bean
    public GroupedOpenApi v1Api() {
        return groupedApi("v1", "/api/v1");
    }

    /**
     * v2 버전의 API 문서 그룹(GroupedOpenApi)을 생성하여 반환합니다.
     *
     * 이 그룹은 이름 "v2"를 사용하고 경로 접두사 "/api/v2" 아래의 모든 엔드포인트를 포함하도록 구성됩니다.
     *
     * @return v2 그룹에 대응하는 GroupedOpenApi 인스턴스
     */
    @Bean
    public GroupedOpenApi v2Api() {
        return groupedApi("v2", "/api/v2");
    }

    /**
     * 지정한 그룹 이름과 전체 경로 접두사로 GroupedOpenApi 인스턴스를 생성한다.
     *
     * 생성된 GroupedOpenApi는 fullPrefix 이하의 모든 경로(fullPrefix/**)에 매칭되며,
     * stripPrefixAndSetServer(fullPrefix) 커스터마이저를 적용해 문서 내 경로에서 접두사를 제거하고 서버 URL을 설정한다.
     *
     * @param group 문서화될 API 그룹 이름 (예: "v1")
     * @param fullPrefix 그룹에 대응되는 전체 경로 접두사 (예: "/api/v1")
     * @return 구성된 GroupedOpenApi 인스턴스
     */
    private GroupedOpenApi groupedApi(String group, String fullPrefix) {
        return GroupedOpenApi.builder()
                .group(group)
                .pathsToMatch(fullPrefix + "/**")
                .addOpenApiCustomizer(stripPrefixAndSetServer(fullPrefix))
                .build();
    }

    /**
     * 그룹화된 API 문서에서 엔드포인트 경로의 그룹 접두사를 제거하고 해당 그룹의 서버 URL을 설정하는 OpenApiCustomizer를 생성한다.
     *
     * <p>동작:
     * <ul>
     *   <li>openApi.getPaths()가 null이거나 비어 있으면 아무 작업도 수행하지 않는다.</li>
     *   <li>각 경로에 대해
     *     <ul>
     *       <li>경로가 fullPrefix와 정확히 같으면 "/"로 매핑한다.</li>
     *       <li>경로가 fullPrefix + "/"로 시작하면 접두사 부분을 제거한다.</li>
     *       <li>그 외의 경로는 그대로 유지된다.</li>
     *     </ul>
     *   </li>
     *   <li>변환된 경로 집합으로 OpenAPI의 paths를 교체하고, servers를 fullPrefix를 URL로 갖는 단일 Server로 설정한다.</li>
     * </ul>
     *
     * @param fullPrefix 경로에서 제거할 그룹 접두사이자 설정할 서버 URL (예: "/api/v1")
     * @return 접두사 제거 및 서버 설정을 적용하는 OpenApiCustomizer
     */
    private OpenApiCustomizer stripPrefixAndSetServer(String fullPrefix) {
        return openApi -> {
            Paths src = openApi.getPaths();
            if (src == null || src.isEmpty()) {
                return;
            }

            Paths dst = new Paths();
            src.forEach((path, item) -> {
                String p = path;
                if (p.equals(fullPrefix)) {
                    p = "/";
                } else if (p.startsWith(fullPrefix + "/")) {
                    p = p.substring(fullPrefix.length());
                }
                dst.addPathItem(p, item);
            });

            openApi.setPaths(dst);
            openApi.setServers(List.of(new Server().url(fullPrefix)));
        };
    }
}
