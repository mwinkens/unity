/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@Component
public class ServiceFileConfigurationController
{
	private final UnityServerConfiguration config;
	private final ServerManagement serverMan;
	private final MessageSource msg;

	@Autowired
	public ServiceFileConfigurationController(UnityServerConfiguration config, ServerManagement serverMan, MessageSource msg)
	{
		this.config = config;
		this.serverMan = serverMan;
		this.msg = msg;
	}

	public EndpointConfiguration getEndpointConfig(String name) throws ControllerException
	{
		Optional<String> endpointKey = getEndpointConfigKey(name);

		if (!endpointKey.isPresent())
		{
			throw new ControllerException(msg.getMessage("ServicesController.getConfigError", name), "", null);
		}

		String description = config.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_DESCRIPTION);
		List<String> authn = config.getEndpointAuth(endpointKey.get());

		String realm = config.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_REALM);
		I18nString displayedName = config.getLocalizedString(msg,
				endpointKey + UnityServerConfiguration.ENDPOINT_DISPLAYED_NAME);
		if (displayedName.isEmpty())
			displayedName.setDefaultValue(name);
		String jsonConfig;
		try
		{
			jsonConfig = serverMan.loadConfigurationFile(
					config.getValue(endpointKey.get() + UnityServerConfiguration.ENDPOINT_CONFIGURATION));
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.getConfigError", name), e);
		}

		return new EndpointConfiguration(displayedName, description, authn, jsonConfig, realm);
	}

	public Optional<String> getEndpointConfigKey(String endpointName)
	{
		for (String endpoint : config.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS))
		{
			String cname = config.getValue(endpoint + UnityServerConfiguration.ENDPOINT_NAME);
			if (endpointName.equals(cname))
			{
				return Optional.of(endpoint);
			}
		}
		return Optional.empty();
	}
}
