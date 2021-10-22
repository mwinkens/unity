/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services;

import java.util.List;

import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.endpoint.Endpoint.EndpointState;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

/**
 * Contains information necessary for create or update service
 * 
 * @author P.Piernik
 *
 */
public class DefaultServiceDefinition implements ServiceDefinition
{
	private String typeId;
	private String name;
	private String address;
	private I18nString displayedName;
	private String description;
	private List<String> authenticationOptions;
	private String configuration;
	private String realm;
	private EndpointState state;
	private String binding;
	private boolean supportFromConfigReload;

	public DefaultServiceDefinition()
	{
		displayedName = new I18nString();
	}

	public DefaultServiceDefinition(ResolvedEndpoint base)
	{
		setTypeId(base.getType().getName());
		setConfiguration(base.getEndpoint().getConfiguration().getConfiguration());
		setAddress(base.getEndpoint().getContextAddress());
		setDescription(base.getEndpoint().getConfiguration().getDescription());
		setDisplayedName(base.getEndpoint().getConfiguration().getDisplayedName());
		setRealm(base.getRealm().getName());
		setAuthenticationOptions(base.getEndpoint().getConfiguration().getAuthenticationOptions());
		setName(base.getName());
		setState(base.getEndpoint().getState());
		setBinding(base.getType().getSupportedBinding());
	}

	public DefaultServiceDefinition(String type)
	{
		this();
		typeId = type;
	}

	public String getType()
	{
		return typeId;
	}

	public void setTypeId(String typeId)
	{
		this.typeId = typeId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getAddress()
	{
		return address;
	}

	public void setAddress(String address)
	{
		this.address = address;
	}

	public I18nString getDisplayedName()
	{
		return displayedName;
	}

	public void setDisplayedName(I18nString displayedName)
	{
		this.displayedName = displayedName;
	}

	public String getDescription()
	{
		return description == null ? "" : description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public List<String> getAuthenticationOptions()
	{
		return authenticationOptions;
	}

	public void setAuthenticationOptions(List<String> authenticationOptions)
	{
		this.authenticationOptions = authenticationOptions;
	}

	public String getConfiguration()
	{
		return configuration;
	}

	public void setConfiguration(String configuration)
	{
		this.configuration = configuration;
	}

	public String getRealm()
	{
		return realm;
	}

	public void setRealm(String realm)
	{
		this.realm = realm;
	}

	@Override
	public EndpointState getState()
	{
		return state;
	}

	@Override
	public String getBinding()
	{
		return binding;
	}

	public void setState(EndpointState state)
	{
		this.state = state;
	}

	public void setBinding(String binding)
	{
		this.binding = binding;
	}
	
	@Override
	public boolean supportFromConfigReload()
	{
		return supportFromConfigReload;
	}

	public void setSupportFromConfigReload(boolean supportFromConfigReload)
	{
		this.supportFromConfigReload = supportFromConfigReload;
	}
}
