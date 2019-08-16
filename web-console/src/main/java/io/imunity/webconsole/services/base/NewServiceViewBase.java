/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.services.base;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.ViewWithSubViewBase;
import io.imunity.webelements.helpers.NavigationHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.services.ServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * 
 * @author P.Piernik
 *
 */

public abstract class NewServiceViewBase extends ViewWithSubViewBase
{
	private UnityMessageSource msg;
	private ServiceControllerBase controller;
	private MainServiceEditor editor;
	private String mainServicesViewName;

	
	public NewServiceViewBase(UnityMessageSource msg, ServiceControllerBase controller, String mainServicesViewName)
	{
		this.msg = msg;
		this.controller = controller;
		this.mainServicesViewName = mainServicesViewName;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		try
		{
			editor = controller.getEditor(null, ServiceEditorTab.GENERAL, this);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(mainServicesViewName);
			return;
		}
		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(editor);
		mainView.addComponent(StandardButtonsHelper.buildConfirmNewButtonsBar(msg, () -> onConfirm(),
				() -> onCancel()));
		setMainView(mainView);
	}

	private void onConfirm()
	{

		ServiceDefinition service;
		try
		{
			service = editor.getService();
		} catch (FormValidationException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("NewServiceView.invalidConfiguration"),
					e);
			return;
		}

		try
		{
			controller.deploy(service);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return;
		}

		NavigationHelper.goToView(mainServicesViewName);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(mainServicesViewName);

	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("New");
	}

	@Override
	public abstract String getViewName();
	
}
