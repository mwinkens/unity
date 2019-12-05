/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

public interface AuthnElementConfiguration
{
	PropertiesRepresentation toProperties(UnityMessageSource msg);
}


