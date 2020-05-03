package bat.ke.qq.com.bean;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class Fox implements InitializingBean,DisposableBean {
	public Fox(){
		System.out.println("Constructor===fox===");
	}

	@PostConstruct
	public void init(){
		System.out.println("@PostConstruct==init==");
	}

	@PostConstruct
	public void init2(){
		System.out.println("@PostConstruct==init2==");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("InitializingBean==afterPropertiesSet==");
	}

	public void initxml(){
		System.out.println("xml==initxml==");
	}

	@PreDestroy
	public void preDestroy(){
		System.out.println("@PreDestroy==preDestroy==");
	}

	@Override
	public void destroy() throws Exception {
		System.out.println("DisposableBean==destroy==");
	}

	public void destroyxml(){
		System.out.println("xml==destroyxml==");
	}

}
