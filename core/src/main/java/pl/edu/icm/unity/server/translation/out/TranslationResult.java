/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.out;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Result of output translation. Set of identities and attributes. This class is mutable: actions modify the contents
 * one by one.
 * @author K. Benedyczak
 */
public class TranslationResult
{
	private Collection<Attribute<?>> attributes = new HashSet<Attribute<?>>();
	private Collection<Identity> identities = new ArrayList<Identity>();

	private Collection<Attribute<?>> attributesToPersist = new HashSet<Attribute<?>>();
	private Collection<Identity> identitiesToPersist = new ArrayList<Identity>();
	
	public Collection<Attribute<?>> getAttributes()
	{
		return attributes;
	}
	
	public Collection<Identity> getIdentities()
	{
		return identities;
	}

	public Collection<Attribute<?>> getAttributesToPersist()
	{
		return attributesToPersist;
	}

	public Collection<Identity> getIdentitiesToPersist()
	{
		return identitiesToPersist;
	}
}
