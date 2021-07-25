/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.server;

import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Stores in thread local state related to the HTTP request being served by the thread.
 */
public class HTTPRequestContext
{
	private static ThreadLocal<HTTPRequestContext> threadLocal = new ThreadLocal<>();
	private final String clientIP;
	private final String userAgent;
	private final String sessionId;
	
	public HTTPRequestContext(String clientIP, String userAgent, String sessionId)
	{
		this.clientIP = clientIP;
		this.userAgent = userAgent;
		this.sessionId = sessionId;
	}

	public static void setCurrent(HTTPRequestContext context)
	{
		threadLocal.set(context);
	}
	
	public static HTTPRequestContext getCurrent() throws InternalException
	{
		return threadLocal.get();
	}

	public String getClientIP()
	{
		return clientIP;
	}

	public String getUserAgent()
	{
		return userAgent;
	}
	
	public String getSessionId()
	{
		return sessionId;
	}
}
