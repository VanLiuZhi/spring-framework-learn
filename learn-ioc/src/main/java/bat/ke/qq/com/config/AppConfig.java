package bat.ke.qq.com.config;

import bat.ke.qq.com.bean.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

@ComponentScan("bat.ke.qq.com")
@Configuration
//@Import(AppConfig2.class)
//@Import(value = {MyImportBeanDefinitionRegistrar.class,MyImportSelector.class})
public class AppConfig {

	/*@Bean
	public User user(){
		return new User();
	}*/

	@Bean(initMethod="initxml",destroyMethod = "destroyxml")
	public Fox fox(){
		return new Fox();
	}
}
