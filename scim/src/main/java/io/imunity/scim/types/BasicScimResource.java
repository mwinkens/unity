/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.types;

import static org.springframework.util.Assert.notEmpty;
import static org.springframework.util.Assert.notNull;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(
{ "schemas", "id", "externalId" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BasicScimResource
{
	public final String id;
	public final String externalId;
	public final Meta meta;
	public final Schemas schemas;

	protected BasicScimResource(BasicScimResourceBuilder<?> builder)
	{
		this.id = builder.id;
		this.externalId = builder.externalId;
		this.meta = builder.meta;
		this.schemas = builder.schemas;
	}


	@Override
	public int hashCode()
	{
		return Objects.hash(externalId, id, meta, schemas);
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
		BasicScimResource other = (BasicScimResource) obj;
		return Objects.equals(externalId, other.externalId) && Objects.equals(id, other.id)
				&& Objects.equals(meta, other.meta) && Objects.equals(schemas, other.schemas);
	}

	public static class BasicScimResourceBuilder<T extends BasicScimResourceBuilder<?>>
	{
		private String id;
		private String externalId;
		private Meta meta;
		private Schemas schemas;

		protected BasicScimResourceBuilder()
		{
		}

		@SuppressWarnings("unchecked")
		public T withId(String id)
		{
			this.id = id;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withExternalId(String externalId)
		{
			this.externalId = externalId;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withMeta(Meta meta)
		{
			this.meta = meta;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withSchemas(Schemas schemas)
		{
			this.schemas = schemas;
			return (T) this;
		}

		public BasicScimResource build()
		{
			notNull(id, "id cannot be null.");
			notNull(schemas, "schemas cannot be null.");
			notEmpty(schemas, "schemas cannot be empty.");
			return new BasicScimResource(this);
		}
	}
}
