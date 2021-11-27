/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.events;

import java.lang.reflect.Method;
import java.util.Date;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.event.PersistableEvent;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

/**
 * Aspect producing events whenever engine method is invoked (implementation from interface with 
 * {@link InvocationEventProducer} annotation).
 * @author K. Benedyczak
 */
@Component
@Aspect
public class EventProducingAspect
{
	public static final String CATEGORY_INVOCATION = "methodInvocation";
	
	private final EventProcessor eventProcessor;
	private final boolean enable;
	
	
	public EventProducingAspect(EventProcessor eventProcessor, UnityServerConfiguration config)
	{
		this.eventProcessor = eventProcessor;
		enable = config.getBooleanValue(UnityServerConfiguration.ENABLE_LOW_LEVEL_EVENTS);
	}
	
	@After("execution(public * pl.edu.icm.unity.engine.api..*.*(..)) && "
			+ "@within(eventProducer)")
	private void afterFinishedByClass(JoinPoint jp, InvocationEventProducer eventProducer)
	{
		if (enable)
			publishOKEvent(jp, eventProducer);
	};
	
	@After("execution(public * pl.edu.icm.unity.engine.api..*.*(..)) && "
			+ "@annotation(eventProducer)")
	public void afterFinishedByMethod(JoinPoint jp, InvocationEventProducer eventProducer)
	{
		if (enable)
			publishOKEvent(jp, eventProducer);
	}

	@AfterThrowing(pointcut="execution(public * pl.edu.icm.unity.engine.api..*.*(..)) && "
			+ "@within(eventProducer)",
			throwing="ex")
	public void afterErrorByClass(JoinPoint jp, InvocationEventProducer eventProducer, Exception ex) 
	{
		if (enable)
			publishExceptionEvent(jp, ex, eventProducer);
	}

	@AfterThrowing(pointcut="execution(public * pl.edu.icm.unity.engine.api..*.*(..)) && "
			+ "@annotation(eventProducer)",
			throwing="ex")
	public void afterErrorByMethod(JoinPoint jp, InvocationEventProducer eventProducer, Exception ex) 
	{
		if (enable)
			publishExceptionEvent(jp, ex, eventProducer);
	}
	
	private void publishOKEvent(JoinPoint jp, InvocationEventProducer eventProducer)
	{
		if (!InvocationContext.hasCurrent())
			return;
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		Long invoker = ae == null ? null : ae.getEntityId();
		MethodSignature signature = (MethodSignature) jp.getSignature();
		PersistableEvent event = new PersistableEvent(CATEGORY_INVOCATION + "." + signature.getMethod().getName(),
				invoker, new Date());
		event.setContents(getMethodDescription(signature.getMethod(), null, 
				signature.getDeclaringType().getSimpleName(), jp.getArgs()));
		eventProcessor.fireEvent(event);
	}

	private void publishExceptionEvent(JoinPoint jp, Exception e, InvocationEventProducer eventProducer)
	{
		if (!InvocationContext.hasCurrent())
			return;
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		Long invoker = ae == null ? null : ae.getEntityId();
		MethodSignature signature = (MethodSignature) jp.getSignature();
		PersistableEvent event = new PersistableEvent(CATEGORY_INVOCATION + "." + signature.getMethod().getName(),
				invoker, new Date());
		event.setContents(getMethodDescription(signature.getMethod(), e.toString(), 
				signature.getDeclaringType().getSimpleName(), jp.getArgs()));
		eventProcessor.fireEvent(event);
	}
	
	private String getMethodDescription(Method method, String exception, String interfaceName, 
			Object[] args)
	{
		InvocationEventContents desc = new InvocationEventContents(method.getName(), 
				interfaceName, args, exception);
		return desc.toJson();
	}
}
