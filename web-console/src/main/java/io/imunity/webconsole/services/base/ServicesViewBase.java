/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.services.base;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.types.endpoint.Endpoint.EndpointState;
import pl.edu.icm.unity.webui.authn.services.ServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.webui.authn.services.ServiceTypeInfoHelper;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * 
 * @author P.Piernik
 *
 */

public abstract class ServicesViewBase extends CustomComponent implements UnityView
{
	protected UnityMessageSource msg;
	private ServiceControllerBase controller;
	private GridWithActionColumn<ServiceDefinition> servicesGrid;
	private String newServiceViewName;
	private String editServiceViewName;
	
	public ServicesViewBase(UnityMessageSource msg, ServiceControllerBase controller, String newServiceViewName, String editServiceViewName)
	{
		this.msg = msg;
		this.controller = controller;
		this.newServiceViewName = newServiceViewName;
		this.editServiceViewName = editServiceViewName;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{

		HorizontalLayout buttonsBar = StandardButtonsHelper.buildTopButtonsBar(StandardButtonsHelper
				.build4AddAction(msg, e -> NavigationHelper.goToView(newServiceViewName)));
		servicesGrid = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
		servicesGrid.addComponentColumn(
				e -> StandardButtonsHelper.buildLinkButton(e.getName(), ev -> gotoEdit(e)),
				msg.getMessage("ServicesView.nameCaption"), 10).setSortable(true)
				.setComparator((e1, e2) -> {
					return e2.getName().compareTo(e1.getName());
				}).setId("name");

		servicesGrid.addComponentColumn(e -> getStatusLabel(e.getState()),
				msg.getMessage("ServicesView.statusCaption"), 10);

		servicesGrid.addColumn(e -> ServiceTypeInfoHelper.getType(msg, e.getType()), msg.getMessage("ServicesView.typeCaption"), 10);

		servicesGrid.addColumn(e -> ServiceTypeInfoHelper.getBinding(msg, e.getBinding()),
				msg.getMessage("ServicesView.bindingCaption"), 10);

		servicesGrid.addHamburgerActions(getHamburgerActionsHandlers());

		servicesGrid.setItems(getServices());
		servicesGrid.sort("name");

		VerticalLayout main = new VerticalLayout();
		main.addComponent(buttonsBar);
		main.addComponent(servicesGrid);
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);
		setCompositionRoot(main);

	}

	private Label getStatusLabel(EndpointState state)
	{
		Label l = new Label();
		l.setContentMode(ContentMode.HTML);
		l.setValue(state.equals(EndpointState.DEPLOYED)? Images.ok.getHtml() : Images.reject.getHtml());
		l.setDescription(state.toString());
		return l;
	}
	
	private Collection<ServiceDefinition> getServices()
	{
		try
		{
			return controller.getServices();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return Collections.emptyList();
		}
	}

	void refresh()
	{
		Collection<ServiceDefinition> services = getServices();
		servicesGrid.setItems(services);
	}

	protected abstract List<SingleActionHandler<ServiceDefinition>> getActionsHandlers();
	
	
	private List<SingleActionHandler<ServiceDefinition>> getHamburgerActionsHandlers()
	{
		SingleActionHandler<ServiceDefinition> remove = SingleActionHandler.builder4Delete(msg, ServiceDefinition.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();
		SingleActionHandler<ServiceDefinition> deploy = SingleActionHandler.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("ServicesView.deploy")).withIcon(Images.play.getResource())
				.withDisabledPredicate(e -> e.getState().equals(EndpointState.DEPLOYED))
				.withHandler(r -> deploy(r.iterator().next())).build();
		SingleActionHandler<ServiceDefinition> undeploy = SingleActionHandler.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("ServicesView.undeploy"))
				.withDisabledPredicate(e -> e.getState().equals(EndpointState.UNDEPLOYED))
				.withIcon(Images.undeploy.getResource()).withHandler(r -> undeploy(r.iterator().next()))
				.build();
		return Arrays.asList(remove, deploy, undeploy);

	}

	private void undeploy(ServiceDefinition endpoint)
	{
		try
		{
			controller.undeploy(endpoint);
			refresh();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private void deploy(ServiceDefinition endpoint)
	{
		try
		{
			controller.deploy(endpoint);
			refresh();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private void tryRemove(ServiceDefinition endpoint)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg,
				Arrays.asList(endpoint.getName()));

		new ConfirmDialog(msg, msg.getMessage("ServicesView.confirmDelete", confirmText),
				() -> remove(endpoint)).show();
	}

	private void remove(ServiceDefinition endpoint)
	{
		try
		{
			controller.remove(endpoint);
			refresh();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}

	}

	private void gotoEdit(ServiceDefinition next)
	{
		gotoEdit(next, ServiceEditorTab.GENERAL);
	}

	protected void gotoEdit(ServiceDefinition next, ServiceEditorTab tab)
	{
		NavigationHelper.goToView(editServiceViewName + "/" + CommonViewParam.name.toString() + "="
				+ next.getName() + "&" + CommonViewParam.tab.toString() + "="
				+ tab.toString().toLowerCase());
	}

	@Override
	public abstract String getViewName();

	@Override
	public abstract String getDisplayedName();
}
