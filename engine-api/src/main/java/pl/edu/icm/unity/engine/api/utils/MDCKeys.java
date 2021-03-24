/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils;

public enum MDCKeys
{
	ENDPOINT("endpoint"),
	USER("user"),
	ENTITY_ID("entityId"),
	CLIENT_IP("clientIP");
	
	public final String key;

	private MDCKeys(String key)
	{
		this.key = key;
	}
}
