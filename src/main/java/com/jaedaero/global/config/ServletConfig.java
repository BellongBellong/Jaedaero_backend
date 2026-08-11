package com.jaedaero.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * 🌐 Spring MVC 웹 컨텍스트 설정 클래스
 * - Spring MVC의 웹 계층(프레젠테이션 계층)을 담당하는 컨텍스트 설정 클래스
 * - 사용자 요청 처리와 관련된 모든 웹 컴포넌트들을 관리하고 설정함
 */
@Configuration
@EnableWebMvc
@ComponentScan(
  basePackages = {"com.jaedaero.domain", "com.jaedaero.global.common.exception"},
  useDefaultFilters = false,
  includeFilters = {
    @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Controller.class),
    @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = RestControllerAdvice.class)
  }
)
public class ServletConfig implements WebMvcConfigurer {

    /**
     * RootConfig와 별개의 서블릿(자식) 컨텍스트라, 여기서도 직접 등록해야 이 컨텍스트의 빈(Controller 등)에서
     * {@code @Value("${...}")}가 application.properties/application-local.properties를 읽는다.
     * 등록하지 않으면 placeholder가 System 프로퍼티만 보고 기본값으로 폴백한다.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setLocations(
                new ClassPathResource("application.properties"),
                new ClassPathResource("application-local.properties")
        );
        configurer.setIgnoreResourceNotFound(true);
        configurer.setLocalOverride(true);
        return configurer;
    }

    /**
     * "/" 요청 시 /resources/index.html로 포워드 설정
     * @param registry
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/")
                .setViewName("forward:/resources/index.html");

        // Springfox 3 Swagger UI 진입 경로입니다.
        registry.addViewController("/swagger-ui.html")
                .setViewName("redirect:/swagger-ui/index.html");
    }

    /**
     * 📁 정적 자원 핸들러 설정
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /resources/** 경로를 위한 핸들러 추가
        registry
                .addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");

        // 프론트엔드 정적 자원 핸들러
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("/resources/assets/");

        // Swagger UI 리소스 핸들러
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations(
                        "classpath:/META-INF/resources/webjars/springfox-swagger-ui/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");

        registry.addResourceHandler("/swagger-resources/**")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/v2/api-docs")
                .addResourceLocations("classpath:/META-INF/resources/");
    }

    // 📍 Servlet 3.0 파일 업로드 설정
    @Bean
    public MultipartResolver multipartResolver() {
        StandardServletMultipartResolver resolver =
                new StandardServletMultipartResolver();
        return resolver;
    }

    /** JSP 뷰 이름을 WEB-INF 아래 JSP 경로로 매핑합니다. */
    @Bean
    public InternalResourceViewResolver jspViewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".jsp");
        return resolver;
    }
}
