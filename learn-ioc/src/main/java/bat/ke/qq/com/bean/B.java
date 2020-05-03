package bat.ke.qq.com.bean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class B {
	@Autowired
	private A a;

	public B(){}


	public B(A a){

	}

}
