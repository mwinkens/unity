/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.finalization;

import java.util.function.BiConsumer;

import com.vaadin.server.Page;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;


/**
 * Enhancements of the base {@link WorkflowCompletedComponent} with additional logout button link in top right corner.
 * 
 */
public class WorkflowCompletedWithLogoutComponent extends CustomComponent
{
	private String logoutCaption;
	private Runnable logoutProcessor;
	private ImageAccessService imageAccessService;

	public WorkflowCompletedWithLogoutComponent(WorkflowFinalizationConfiguration config, 
			 BiConsumer<Page, String> redirector, String logoutCaption, Runnable logoutProcessor, ImageAccessService imageAccessService)
	{
		this.logoutCaption = logoutCaption;
		this.logoutProcessor = logoutProcessor;
		this.imageAccessService = imageAccessService;
		createUI(config, redirector);
	}
	
	private void createUI(WorkflowFinalizationConfiguration config, BiConsumer<Page, String> redirector)
	{
		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		main.setMargin(true);
		main.setSpacing(false);

		Component logout = createLogoutComponent();
		main.addComponent(logout);
		main.setComponentAlignment(logout, Alignment.TOP_RIGHT);
		
		Component base = new WorkflowCompletedComponent(config, redirector, imageAccessService);
		main.addComponent(base);
		main.setComponentAlignment(base, Alignment.MIDDLE_CENTER);
		main.setExpandRatio(base, 10);
		
		setCompositionRoot(main);
		setSizeFull();
	}
	
	private Component createLogoutComponent()
	{
		Button logout = new Button(logoutCaption);
		logout.addStyleName(Styles.vButtonLink.toString());
		logout.addClickListener(event -> logoutProcessor.run());
		return logout;
	}
}
