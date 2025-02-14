/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.idpcommon;

import java.net.URI;
import java.net.URISyntaxException;

public class URIPresentationHelper
{
	public static String getHumanReadableDomain(String url)
	{
		try
		{
			URI uri = new URI(url);
			String host = uri.getHost();
			return host == null ? url : host;
		} catch (URISyntaxException e)
		{
			return url;
		}
	}
}
