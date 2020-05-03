package bat.ke.qq.com.scope;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * @Description
 * @Author VanLiuZhi
 * @Date 2020-04-14 16:25
 */
// @Configuration
@ComponentScan
public class AppConfig {
    @Bean
    @Scope("singleton")
    public Single singleton(){
        return new Single();
    }
    @Bean
    @Scope("prototype")
    public Prototype prototype(){
        return new Prototype();
    }
}

