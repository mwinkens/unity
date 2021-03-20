/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import java.text.DateFormat;
import java.util.Date;

import org.apache.logging.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.stereotype.Component;

import eu.unicore.util.LoggerFactory;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.utils.LifecycleBase;
import pl.edu.icm.unity.engine.utils.UnityLoggerFactory;


/**
 * Main startup program. Fires up Spring container.
 * @author K. Benedyczak
 */
public class UnityApplication
{
	static 
	{
		System.setProperty(LoggerFactory.LOGGER_FACTORY_PROPERTY, UnityLoggerFactory.class.getName());
	}
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, UnityApplication.class);

	private ClassPathXmlApplicationContext container;

	private String[] activeProfiles;
	
	public UnityApplication(String... activeProfiles)
	{
		this.activeProfiles = activeProfiles;
	}

	public void run(String[] args)
	{
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG);
		System.out.println(df.format(new Date()) + ": Starting UNITY Web Server");
		log.info("\n**************************\nStarting UNITY Web Server\n**************************");
		
		container = new ClassPathXmlApplicationContext(new String[] {"classpath*:META-INF/components.xml"}, false);
		MutablePropertySources propertySources = container.getEnvironment().getPropertySources();
		SimpleCommandLinePropertySource clps = new SimpleCommandLinePropertySource(args);
		propertySources.addFirst(clps);
		ConfigurableEnvironment env = container.getEnvironment();
		env.setActiveProfiles(UnityServerConfiguration.PROFILE_PRODUCTION);
		for (String profile: activeProfiles)
			env.addActiveProfile(profile);
		container.setAllowBeanDefinitionOverriding(false);
		container.refresh();

		container.registerShutdownHook();
		container.start();
	}

	public static void main(String[] args)
	{
		UnityApplication theServer = new UnityApplication();
		theServer.run(args);
	}
	
	
	
	/**
	 * Responsible for logging the end of startup procedure and the start of the shutdown
	 * @author K. Benedyczak
	 */
	@Component		
	public static class InnerServerLifecycle extends LifecycleBase
	{
		@Override
		public void start()
		{
			DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG);
			System.out.println(df.format(new Date()) + ": UNITY Server Started");
			log.info("\n**************************\nUNITY Server Started\n**************************");
			super.start();
		}
		
		@Override
		public void stop()
		{
			DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG);
			System.out.println(df.format(new Date()) + ": Stopping UNITY Server");
			log.info("\n**************************\nStopping UNITY Server\n**************************");
			super.stop();
		}

		@Override
		public int getPhase()
		{
			return Integer.MAX_VALUE;
		}
	}

	/**
	 * Responsible for logging the end of shutdown operation
	 * @author K. Benedyczak
	 */
	@Component
	public static class ShutdownFinished extends LifecycleBase
	{
		@Override
		public void stop()
		{
			DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG);
			System.out.println(df.format(new Date()) + ": Stopped UNITY Server");
			log.info("\n**************************\nStopped UNITY Server\n**************************");
			super.stop();
		}

		@Override
		public int getPhase()
		{
			return Integer.MIN_VALUE;
		}
	}
}
