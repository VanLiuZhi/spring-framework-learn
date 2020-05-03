package bat.ke.qq.com.test;

// import bat.ke.qq.com.bean.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author VanLiuZhi
 * @Date 2020-04-09 15:53
 */
@Component
public class UserComponent implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		System.out.println("hello myRegisterBean A");
		BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(A.class).getBeanDefinition();
		registry.registerBeanDefinition("myRegisterBean A", beanDefinition);
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}

	@Override
	public int getOrder() {
		return 20;
	}
}
