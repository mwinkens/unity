/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup.attributeTypes;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;
import pl.edu.icm.unity.webui.common.grid.FilterableEntry;

/**
 * Represent grid attribute type entry.
 * 
 * @author P.Piernik
 *
 */
class AttributeTypeEntry implements FilterableEntry
{
	public final AttributeType attributeType;
	private MessageSource msg;

	AttributeTypeEntry(MessageSource msg, AttributeType attributeType)
	{
		this.attributeType = attributeType;
		this.msg = msg;
	}

	boolean isEditable()
	{
		return !(attributeType.isTypeImmutable() && attributeType.isInstanceImmutable());
	}

	String getDisplayedName()
	{
		return attributeType.getDisplayedName().getValue(msg);
	}

	String getBoundsDesc()
	{
		return AttributeTypeUtils.getBoundsDesc(msg, attributeType.getMinElements(),
				attributeType.getMaxElements());
	}

	String getDescription()
	{
		return attributeType.getDescription().getValue(msg);
	}

	@Override
	public boolean anyFieldContains(String searched, MessageSource msg)
	{
		String textLower = searched.toLowerCase();

		if (getDisplayedName() != null && getDisplayedName().toLowerCase().contains(textLower))
			return true;

		if (attributeType.getName() != null && attributeType.getName().toLowerCase().contains(textLower))
			return true;

		if (attributeType.getValueSyntax() != null
				&& attributeType.getValueSyntax().toLowerCase().contains(textLower))
			return true;

		if (getBoundsDesc() != null && getBoundsDesc().toLowerCase().contains(textLower))
			return true;

		if (getBoundsDesc() != null && getBoundsDesc().toLowerCase().contains(textLower))
			return true;

		return false;
	}

}
