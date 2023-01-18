/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

class RestMembershipEnquiry
{
	public final String name;
	public final boolean autogenerate;

	@JsonCreator
	RestMembershipEnquiry(@JsonProperty("name") String name, @JsonProperty("autogenerate") boolean autogenerate)
	{
		this.name = name;
		this.autogenerate = autogenerate;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RestMembershipEnquiry that = (RestMembershipEnquiry) o;
		return autogenerate == that.autogenerate && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, autogenerate);
	}

	@Override
	public String toString()
	{
		return "RestMembershipEnquiry{" +
			"name='" + name + '\'' +
			", autogenerate=" + autogenerate +
			'}';
	}
}
