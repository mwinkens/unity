/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.config;

import java.util.Objects;

public class TrustedIdPKey
{
	private final String key;

	public TrustedIdPKey(String key)
	{
		this.key = key;
	}

	public String asString()
	{
		return key;
	}
	
	@Override
	public String toString()
	{
		return String.format("TrustedIdPKey [key=%s]", key);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(key);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrustedIdPKey other = (TrustedIdPKey) obj;
		return Objects.equals(key, other.key);
	}
}
