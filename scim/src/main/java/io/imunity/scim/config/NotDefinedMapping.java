/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotDefinedMapping implements AttributeMapping
{
	public static final String id = "NotDefined";

	@Override
	public Optional<DataArray> getDataArray()
	{
		return Optional.empty();
	}

	@Override
	public String getEvaluatorId()
	{
		return id;
	}

}
