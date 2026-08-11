package com.jaedaero.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Clock;
import java.time.ZoneId;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 🌱 루트 애플리케이션 컨텍스트 설정 클래스
 * - Spring Framework의 최상위(루트) 애플리케이션 컨텍스트를 설정하는 클래스
 * - 웹 계층과 무관한 비즈니스 로직, 서비스, 데이터 접근 계층의 빈을 관리
 */
@Slf4j
@Configuration
@EnableTransactionManagement
@EnableScheduling
@MapperScan(basePackages = "com.jaedaero.domain", annotationClass = Mapper.class)
@ComponentScan(
        basePackages = {"com.jaedaero.domain", "com.jaedaero.global.security"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = Controller.class
        )
)
@Import(DotenvConfig.class)
public class RootConfig {

    @Bean
    public Clock applicationClock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }

    /**
     * @PropertySource에 등록한 속성을 @Value 표현식에서 해석한다.
     * 정적 빈으로 등록해야 설정 클래스 초기화 이전에도 적용된다.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer =
                new PropertySourcesPlaceholderConfigurer();

        configurer.setLocations(
                new ClassPathResource("application.properties"),
                new ClassPathResource("application-local.properties")
        );
        configurer.setIgnoreResourceNotFound(true);
        return configurer;
    }

    @Value("${db.driver}")
    String driver;
    @Value("${db.url}")
    String url;
    @Value("${db.username}")
    String username;
    @Value("${db.password}")
    String password;
    @Autowired
    ApplicationContext applicationContext;


    /**
     * HikariCP 커넥션 풀을 사용한 데이터 소스 빈 생성
     *
     * @return 설정된 데이터 소스 객체
     */
    @Bean
    public DataSource dataSource() {
        // HikariCP 설정 객체 생성
        HikariConfig config = new HikariConfig();

        // 데이터베이스 연결 정보 설정
        config.setDriverClassName(driver);          // JDBC 드라이버 클래스
        config.setJdbcUrl(url);                    // 데이터베이스 URL
        config.setUsername(username);              // 사용자명
        config.setPassword(password);              // 비밀번호

        // 커넥션 풀 추가 설정 (선택사항)
        config.setMaximumPoolSize(10);             // 최대 커넥션 수
        config.setMinimumIdle(5);                  // 최소 유지 커넥션 수
        config.setConnectionTimeout(30000);       // 연결 타임아웃 (30초)
        config.setIdleTimeout(600000);            // 유휴 타임아웃 (10분)

        // Hikari 데이터 소스 생성 및 반환
        HikariDataSource dataSource = new HikariDataSource(config);
        return dataSource;
    }

    /**
     * SQL 세션 팩토리 빈 등록
     * - MyBatis의 핵심 팩토리 객체를 스프링 컨테이너에 등록
     *
     * @param dataSource 위 dataSource() 메서드에서 등록된 빈이 주입됨
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();

        // MyBatis 설정 파일 위치 지정
        sqlSessionFactory.setConfigLocation(applicationContext.getResource("classpath:/mybatis-config.xml"));

        // src/main/resources/mapper/** 아래의 매퍼 XML을 MyBatis에 등록
        sqlSessionFactory.setMapperLocations(applicationContext.getResources("classpath*:mapper/**/*.xml"));

        // 데이터베이스 연결 설정
        sqlSessionFactory.setDataSource(dataSource);

        return sqlSessionFactory.getObject();
    }

    /**
     * 트랜잭션 매니저 설정
     * - 데이터베이스 트랜잭션을 스프링이 관리하도록 설정
     */
    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(10000);
        return new RestTemplate(requestFactory);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

}
