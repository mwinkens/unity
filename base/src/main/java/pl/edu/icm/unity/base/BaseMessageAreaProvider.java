/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.base;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageArea;
import pl.edu.icm.unity.msg.MessageAreaProvider;

@Component
public class BaseMessageAreaProvider implements MessageAreaProvider
{
	public final String NAME = "base";

	@Override
	public MessageArea getMessageArea()
	{
		return new MessageArea(NAME, "BaseMessageAreaProvider.displayedName", false);
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
