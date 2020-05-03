package bat.ke.qq.com.test;

import bat.ke.qq.com.bean.Cat;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * @Description
 * @Author VanLiuZhi
 * @Date 2020-03-13 17:27
 */
@Configuration
@Order(5)
public class User implements BeanDefinitionRegistryPostProcessor, Ordered {
	@Bean
	public A a() {
		return new A();
	}

	@Bean
	public B b() {
		return new B(a());
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(Cat.class).getBeanDefinition();
		registry.registerBeanDefinition("myRegisterBean Cat", beanDefinition);
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}

	@Override
	public int getOrder() {
		return 0;
	}
}

