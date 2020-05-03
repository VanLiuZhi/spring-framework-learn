package bat.ke.qq.com.common;

import bat.ke.qq.com.bean.Cat;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class MyApplicationContextAware implements FactoryBean<Cat> {

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		System.out.println("aware");
		// System.out.println(applicationContext);
	}

	@Override
	public Cat getObject() throws Exception {
		System.out.println("create cat");
		return new Cat();
	}

	@Override
	public Class<?> getObjectType() {
		return null;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}
}
