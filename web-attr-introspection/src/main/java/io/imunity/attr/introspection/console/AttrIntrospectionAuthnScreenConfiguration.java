/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console;

import static pl.edu.icm.unity.webui.VaadinEndpointProperties.PREFIX;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Properties;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.FileStorageService.StandardOwner;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.file.FileFieldUtils;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.AuthnLayoutColumnConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.AuthnLayoutConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.AuthnLayoutPropertiesParser;

public class AttrIntrospectionAuthnScreenConfiguration
{
	private AuthnLayoutConfiguration authnLayoutConfiguration;
	private boolean enableSearch;
	private LocalOrRemoteResource logo;
	private I18nString title;
	

	AttrIntrospectionAuthnScreenConfiguration()
	{
		authnLayoutConfiguration = new AuthnLayoutConfiguration(
				Arrays.asList(new AuthnLayoutColumnConfiguration(new I18nString(),
						(int)VaadinEndpointProperties.DEFAULT_AUTHN_COLUMN_WIDTH,
						Lists.newArrayList())),
				Lists.newArrayList());
	}
	
	Properties toProperties(MessageSource msg, FileStorageService fileStorageService,
			String serviceName)
	{
		Properties raw = new Properties();
		raw.put(PREFIX + VaadinEndpointProperties.AUTHN_SHOW_SEARCH, String.valueOf(enableSearch));

		if (title != null)
			title.toProperties(raw, PREFIX + VaadinEndpointProperties.AUTHN_TITLE, msg);

		if (logo != null)
		{
			FileFieldUtils.saveInProperties(getLogo(),
					PREFIX + VaadinEndpointProperties.AUTHN_LOGO, raw,
					fileStorageService, StandardOwner.SERVICE.toString(), serviceName);
		} else
		{
			raw.put(PREFIX + VaadinEndpointProperties.AUTHN_LOGO, "");
		}
		
		
		AuthnLayoutPropertiesParser parser = new AuthnLayoutPropertiesParser(msg);
		raw.putAll(parser.toProperties(authnLayoutConfiguration));

		return raw;
	}

	void fromProperties(String vaadinProperties, MessageSource msg, ImageAccessService imageAccessService)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(vaadinProperties));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the attribute introspection service", e);
		}

		VaadinEndpointProperties vProperties = new VaadinEndpointProperties(raw);
		fromProperties(vProperties, msg, imageAccessService);
	}

	private void fromProperties(VaadinEndpointProperties vaadinProperties, MessageSource msg, ImageAccessService imageAccessService)
	{
		AuthnLayoutPropertiesParser parser = new AuthnLayoutPropertiesParser(msg);
		authnLayoutConfiguration = parser.fromProperties(vaadinProperties);
		enableSearch = vaadinProperties.getBooleanValue(VaadinEndpointProperties.AUTHN_SHOW_SEARCH);
		String logoUri = vaadinProperties.getValue(VaadinEndpointProperties.AUTHN_LOGO);
		
		logo = imageAccessService.getEditableImageResourceFromUri(logoUri, 
				vaadinProperties.getEffectiveAuthenticationTheme()).orElse(null);

		title = vaadinProperties.getLocalizedStringWithoutFallbackToDefault(msg,
				VaadinEndpointProperties.AUTHN_TITLE);
		if (title.isEmpty())
			title = null;
	}

	public AuthnLayoutConfiguration getauthnLayoutConfiguration()
	{
		return authnLayoutConfiguration;
	}

	public void setauthnLayoutConfiguration(AuthnLayoutConfiguration authnLayoutConfiguration)
	{
		this.authnLayoutConfiguration = authnLayoutConfiguration;
	}

	public boolean isEnableSearch()
	{
		return enableSearch;
	}

	public void setEnableSearch(boolean enableSearch)
	{
		this.enableSearch = enableSearch;
	}

	public LocalOrRemoteResource getLogo()
	{
		return logo;
	}

	public void setLogo(LocalOrRemoteResource logo)
	{
		this.logo = logo;
	}

	public I18nString getTitle()
	{
		return title;
	}

	public void setTitle(I18nString title)
	{
		this.title = title;
	}

}
