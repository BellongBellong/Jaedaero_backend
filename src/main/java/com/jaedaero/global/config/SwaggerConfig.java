package com.jaedaero.global.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

  @Bean
  public Docket jaedaeroApi() {
    return new Docket(DocumentationType.SWAGGER_2)
        .apiInfo(apiInfo())
        .select()
        .apis(RequestHandlerSelectors.basePackage("com.jaedaero"))
        .paths(PathSelectors.any())
        .build()
        .securitySchemes(List.of(new ApiKey("BearerAuth", "Authorization", "header")))
        .securityContexts(List.of(securityContext()));
  }

  private ApiInfo apiInfo() {
    return new ApiInfoBuilder()
        .title("제대로 Backend API")
        .description("군인의 확정소득 기반 금융 성장 플랫폼 API")
        .version("v1")
        .build();
  }

  private SecurityContext securityContext() {
    AuthorizationScope authorizationScope =
        new AuthorizationScope("global", "JWT 액세스 토큰이 필요한 API에 사용합니다.");
    SecurityReference securityReference =
        new SecurityReference("BearerAuth", new AuthorizationScope[] {authorizationScope});
    return SecurityContext.builder()
        .securityReferences(List.of(securityReference))
        .forPaths(PathSelectors.regex("/api/v1/(?!auth/login|auth/refresh).*"))
        .build();
  }
}
