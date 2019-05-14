/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.signupAndEnquiry.SignupAndEnquiryNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Lists all invitations
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class InvitationsView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "Invitations";

	private UnityMessageSource msg;
	

	@Autowired
	InvitationsView(UnityMessageSource msg)
	{
		this.msg = msg;
		
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.signupAndEnquiry.invitations");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class InvitationsNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public InvitationsNavigationInfoProvider(UnityMessageSource msg,
				SignupAndEnquiryNavigationInfoProvider parent,
				ObjectFactory<InvitationsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.signupAndEnquiry.invitations"))
					.withPosition(30).build());

		}
	}
}
