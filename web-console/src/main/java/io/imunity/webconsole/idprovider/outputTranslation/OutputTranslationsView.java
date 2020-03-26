/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.idprovider.outputTranslation;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.idprovider.IdentityProviderNavigationInfoProvider;
import io.imunity.webconsole.translationsProfiles.TranslationsView;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Lists all output translation profiles
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class OutputTranslationsView extends TranslationsView
{
	public static final String VIEW_NAME = "OutputTranslations";

	@Autowired
	public OutputTranslationsView(UnityMessageSource msg, OutputTranslationsController controller)
	{
		super(msg, controller);
	}

	@Override
	protected String getEditView()
	{
		return EditOutputTranslationView.VIEW_NAME;
	}

	@Override
	protected String getNewView()
	{
		return NewOutputTranslationView.VIEW_NAME;
	}

	@Override
	public String getHeaderCaption()
	{
		return msg.getMessage("OutputTranslationsView.headerCaption");
	}

	@Override
	public String getDisplayedName()
	{

		return msg.getMessage("WebConsoleMenu.idpProvider.outputTranslation");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class OutputTranslationsNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		@Autowired
		public OutputTranslationsNavigationInfoProvider(UnityMessageSource msg,
				IdentityProviderNavigationInfoProvider parent,
				ObjectFactory<OutputTranslationsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withIcon(Images.upload.getResource())
					.withCaption(msg.getMessage(
							"WebConsoleMenu.identityProvider.outputTranslation"))
					.withPosition(20).build());

		}
	}
}
