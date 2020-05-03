/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}

	/**
	 * PostProcessorRegistrationDelegate 后置处理器注册增强
	 *
	 * 跟这个最核心的方法，也是最长的，下面我会对一些比较长的英文变量做个大概意思的中文描述，帮助理解
	 *
	 * 方法名invokeBeanFactoryPostProcessors，调用bean工厂的后置处理器，来看看具体做了什么吧
	 *
	 * 到现在为止，主要做了下面2件事:
	 *
	 * 回顾: DefaultListableBeanFactory 工厂实现类，也是最底层的(有一个类继承了它，不过无关紧要)，一个全功能的工厂，bean就在里面
	 *
	 * <span>1. 注册内置bean给工厂，也就是加入 beanDefinitionMap 中</span>

		ConfigurationClassPostProcessor
		AutowiredAnnotationBeanPostProcessor
		CommonAnnotationBeanPostProcessor
		EventListenerMethodProcessor
		DefaultEventListenerFactory

	 * <span>2. 添加2个后置处理器给工厂，这里是把class对象赋值到beanFactory 的 beanPostProcessors 属性中，
	 * 这个是一个CopyOnWriteArrayList类型的属性，数据成员是BeanPostProcessor接口类型的
	 * </span>

		 ApplicationContextAwareProcessor
		 ApplicationListenerDetector

	 * 这两个后置处理器都是BeanPostProcessor接口的实现，不然写不到beanPostProcessors变量中
	 *
	 * 1中的几个bean，继承关系比较复杂
	 * 2中的比较简单，都实现了BeanPostProcessor，ApplicationListenerDetector扩展了一些东西
	 * BeanPostProcessor这个接口有两个方法，也是很重要的，后面会讲，它们是:
	 *
	 * postProcessBeforeInitialization
	 * postProcessAfterInitialization
	 *
	 * 还有一个 BeanFactoryPostProcessor 接口，和 BeanPostProcessor 就差了一个单词，这两个是特别重要的Hook(也就是钩子)
	 *
	 * PS: 从我的经验来看，确实很少有人去聊这些钩子，和Spring的扩展点，好像官网也比较少(不负责任的猜想，怕大家乱搞破坏规范？)
	 * 	   原因可能是Spring体系真的太成熟和标准了，除非是搞中间件和定制企业框架，不然常规的业务很少用这些
	 *
	 * 我们来看看这两个钩子能干什么
	 *
	 * BeanFactoryPostProcessor: 这个接口传递的是 `ConfigurableListableBeanFactory`
	 * BeanFactoryPostProcessor在容器实际实例化任何其它的bean之前读取配置元数据，并有可能修改它
	 *
	 * BeanPostProcessor: 传递的是 `bean` 对象，和 `eanName`
	 * BeanPostProcessor接口可以在Bean(实例化之后)初始化的前后做一些自定义的操作
	 *
	 * 一个是改原数据的，就是能直接拿到工厂，工厂里面你想干什么都可以，BeanPostProcessor就弱一些，它只能拿到bean，也就是BeanDefinition
	 *
	 * 先记住这么多，下面开始跟流程
	 */

	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		Set<String> processedBeans = new HashSet<>();

		// beanFactory是 DefaultListableBeanFactory，是BeanDefinitionRegistry的实现类，所以肯定满足if
		// 可以看看这个接口，BeanDefinitionRegistry声名了一些BeanDefinition的操作

		if (beanFactory instanceof BeanDefinitionRegistry) {

			// 类型转换，那么registry是由接口BeanDefinitionRegistry限定的对象
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

			// regularPostProcessors(有规律，定期的后置处理器) 用来存放BeanFactoryPostProcessor
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();

			// registryProcessors 用来存放BeanDefinitionRegistryPostProcessor
			// BeanDefinitionRegistryPostProcessor 扩展了 BeanFactoryPostProcessor

			// BeanDefinitionRegistryPostProcessor 接口 可以看作是BeanFactoryPostProcessor 和 ImportBeanDefinitionRegistrar 的功能集合，
			// 既可以获取和修改BeanDefinition的元数据，也可以实现BeanDefinition的注册、移除等操作

			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			// 循环传进来的beanFactoryPostProcessors，正常情况下，beanFactoryPostProcessors肯定没有数据
			// 因为beanFactoryPostProcessors是获得手动添加的，而不是spring扫描的
			// 只有手动调用，比如annotationConfigApplicationContext.addBeanFactoryPostProcessor(XXX)才会有数据

			// 个人猜测, 可能会有手动添加的情况，导致下面的逻辑执行

			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				// 判断postProcessor是不是BeanDefinitionRegistryPostProcessor，因为BeanDefinitionRegistryPostProcessor
				// 扩展了BeanFactoryPostProcessor，所以这里先要判断是不是BeanDefinitionRegistryPostProcessor
				// 是的话，直接执行postProcessBeanDefinitionRegistry方法，然后把对象装到registryProcessors里面去
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					registryProcessors.add(registryProcessor);
				}
				// 不是的话，就装到regularPostProcessors
				else {
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.

			// 一个临时变量，用来装载BeanDefinitionRegistryPostProcessor
			// BeanDefinitionRegistry继承了PostProcessorBeanFactoryPostProcessor
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.

			// 获得实现BeanDefinitionRegistryPostProcessor接口的类的
			// BeanName: org.springframework.context.annotation.internalConfigurationAnnotationProcessor（要注意这是个名称，不像一般的是类名首字母小写做名称，它不是一个类）
			// class: ConfigurationClassPostProcessor

			// 并且装入数组postProcessorNames，一般情况下，只会找到一个，如果我们传入的Component也实现了接口，也会被找到
			// 具体实现是，在register(componentClasses);中我们传入的Component已经在BeanDefinitionMap中了，通过getBeanNamesForType方法去反射解析，就能得到了
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);

			// 去容器中查询是否有实现了PriorityOrdered接口的BeanDefinitionRegistryPostProcessor，使用isTypeMatch方法
			// 我们的Component只实现了BeanDefinitionRegistryPostProcessor，没有实现PriorityOrdered，
			// 所以被放到currentRegistryProcessors中的，只有ConfigurationClassPostProcessor
			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					// 获得ConfigurationClassPostProcessor类，并且放到currentRegistryProcessors
					// ConfigurationClassPostProcessor是很重要的一个类，它实现了BeanDefinitionRegistryPostProcessor接口
					// BeanDefinitionRegistryPostProcessor接口又实现了BeanFactoryPostProcessor接口
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					// 把name放到processedBeans，后续会根据这个集合来判断处理器是否已经被执行过了
					processedBeans.add(ppName);
				}
			}
			// 处理排序
			// 上面说了，我们可以实现这个接口BeanDefinitionRegistryPostProcessor添加后置处理器，
			// 如果我们还实现了PriorityOrdered接口，那么该后置处理器执行是有顺序的，
			// 所以上面的for先把需要按顺序执行的找出来

			// PriorityOrdered extends Ordered，然后实现getOrder方法，返回一个int，数值越小优先级越高
			// 这个排序比较繁琐，也不是我们关注的重点，知道排序就行了，继续后面的流程吧

			// 补充一点关于 beanFactory.isTypeMatch 方法，实现了PriorityOrdered接口的，自然也实现了 Ordered 接口，
			// 这里先判断PriorityOrdered接口的。我理解它设计的思想是 Priority 是更高的优先级，然后下面会判断实现 Ordered 接口的
			// 假如多个后置处理器，实现了 PriorityOrdered 接口的，beanFactory.isTypeMatch判断两种类型都是 true
			// 而实现了 Ordered 接口的，beanFactory.isTypeMatch 判断 PriorityOrdered 为 false，再配合processedBeans排除处理过的
			// 完成了一个 `优先级顺序(PriorityOrdered)`，`顺序(Ordered)`，`普通` 的后置处理器执行顺序

			sortPostProcessors(currentRegistryProcessors, beanFactory);

			// 合并Processors，把上面两个for处理后的处理器放入一个集合中，集合都是BeanDefinitionRegistryPostProcessor类型的
			// 为什么要合并，因为registryProcessors是装载BeanDefinitionRegistryPostProcessor的
			// 一开始的时候，spring只会执行BeanDefinitionRegistryPostProcessor独有的方法
			// 而不会执行BeanDefinitionRegistryPostProcessor父类的方法，即BeanFactoryProcessor的方法
			// 所以这里需要把处理器放入一个集合中，后续统一执行父类的方法
			registryProcessors.addAll(currentRegistryProcessors);

			// 可以理解为执行ConfigurationClassPostProcessor的postProcessBeanDefinitionRegistry方法
			// Spring热插播的体现，像ConfigurationClassPostProcessor就相当于一个组件，Spring很多事情就是交给组件去管理
			// 如果不想用这个组件，直接把注册组件的那一步去掉就可以

			// 会调用ConfigurationClassPostProcessor#postProcessBeanDefinitionRegistry 解析注解，注册bean(如果你添加了新的后置处理器，并且是有优先级的，那么也会在这里处理)
			// postProcessBeanDefinitionRegistry 是 BeanDefinitionRegistryPostProcessor 接口的方法，特别重要的一个概念，最开始我们就聊过了
			/**
			 * 核心方法，记住处理的是具有优先级，并且实现了 BeanDefinitionRegistryPostProcessor 的bean
			 * 我们自己定义的后置处理器，如果没有优先级，在后面才去执行
			 * 可以把我们传入的Component，也去实现PriorityOrdered接口(修改UserComponent来测试)，测试一下是否会再次执行(当然是会的)
			 */
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);

			//因为currentRegistryProcessors是一个临时变量，所以需要清除
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.

			// 再次根据BeanDefinitionRegistryPostProcessor获得BeanName，看这个BeanName是否已经被执行过了，有没有实现Ordered接口
			// 如果没有被执行过，也实现了Ordered接口的话，把对象推送到currentRegistryProcessors，名称推送到processedBeans
			// 如果没有实现Ordered接口的话，这里不把数据加到currentRegistryProcessors，processedBeans中，后续再做处理
			// 这里才可以获得我们定义的实现了BeanDefinitionRegistryPostProcessor的Bean

			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				// 排除被处理过的，并且实现了Ordered接口的，priorityOrdered 继承Ordered接口，但是它没做什么事情
				// 此时不包含的是我们传入的User，但是它没有实现Ordered接口，我们修改一下，测试一下效果(当然是修改后的User会进来)
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			// 又把上面做的事情走了一次
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			// 把新加入的User的后置处理器执行一下
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();

			/**
			 * ok，我们屡屡，现在做了的事情
			 *
			 * 上面流程是去处理实现了 BeanDefinitionRegistryPostProcessor 的 bean，官方的叫法是后置处理器
			 * 在还没有进入refresh方法前，DefaultListableBeanFactory 工厂已经装载了Spring的内置bean，和我们传入的Component
			 *
			 * beanDefinitionMap 中，实现了BeanDefinitionRegistryPostProcessor的是 ConfigurationClassPostProcessor
			 * 然后我们添加的Component也去实现了 BeanDefinitionRegistryPostProcessor，所以现在是3个
			 *
			 * 只实现 BeanDefinitionRegistryPostProcessor 还不行，还要实现 priorityOrdered，Ordered，满足这些条件的后置处理器被按优先级顺序优先处理
			 *
			 * BeanDefinitionRegistryPostProcessor 接口是注册和工厂的全功能接口，有下面两个方法:
			 *
			 * 方法 postProcessBeanDefinitionRegistry，参数BeanDefinitionRegistry
			 * 方法 postProcessBeanFactory，参数ConfigurableListableBeanFactory
			 *
			 * 这里又不得不提到 DefaultListableBeanFactory 了，查看它的继承实现，可以很清晰的看到，BeanDefinitionRegistry和ConfigurableListableBeanFactory
			 * 都是它实现的，然后Spring用多态去限定DefaultListableBeanFactory，把工厂的能力一份为二
			 *
			 * 而这里只执行了后置处理器的postProcessBeanDefinitionRegistry，
			 * 然后把后置处理器收集到registryProcessors中，等下面的流程执行postProcessBeanFactory
			 *
			 */

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.

			// 上面的代码是执行了实现了Ordered接口的BeanDefinitionRegistryPostProcessor，
			// 下面的代码就是执行没有实现Ordered接口的BeanDefinitionRegistryPostProcessor

			// 去容器中查询是否有排除实现PriorityOrdered和Ordered后的其他的BeanDefinitionRegistryPostProcessor

			// 这里我们又加了一个UserCommon来模拟测试
			boolean reiterate = true;
			// 这里为什么是while循环？个人理解，是因为 BeanDefinitionRegistryPostProcessor 能够注册 BeanDefinition，
			// 如果再注册了一个实现了 BeanDefinitionRegistryPostProcessor 接口的 BeanDefinition，那么就可以在下次while循环中拿到再处理了
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			// 把上面收集的后置处理器，去执行BeanFactoryPostProcessors接口实现的方法，
			// 至此，实现了BeanDefinitionRegistryPostProcessor的后置处理器全部执行完
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// 下面的流程就和上面的是类似的，取出BeanFactoryPostProcessor类型的，
		// 这里应该是5个，我们的3个，内置的ConfigurationClassPostProcessor
		// 本次新增的EventListenerMethodProcessor，由于processedBeans会判断，所以前4个不会再处理了
		// 最后按照
		// priorityOrderedPostProcessors，
		// orderedPostProcessorNames，
		// nonOrderedPostProcessorNames
		// 来把后置处理器分组，执行对应的BeanFactoryPostProcessors方法，和上面类似，实现了优先级的会先处理
		// 这里没有测试自己新增的后置处理器了，思路和上面都是类似的
		// 另外，BeanFactoryPostProcessors后置处理器没法注册BeanDefinition，所以不需要while循环了

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 * 后置处理器执行方法 BeanDefinitionRegistry
	 * ConfigurationClassPostProcessor 的后置处理器会去扫描并注册bean
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 * 后置处理器执行方法 BeanFactory
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
