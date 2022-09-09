/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.endpoint.common;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

@Component
class CustomErrorPageInitializer implements VaadinServiceInitListener, SessionInitListener
{
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void serviceInit(ServiceInitEvent event)
	{
		event.getSource().addSessionInitListener(this);
	}

	@Override
	public void sessionInit(SessionInitEvent event)
	{
		event.getSession().setErrorHandler(errorEvent ->
		{
			LOG.error("This error occurred, when vaadin has been loaded:", errorEvent.getThrowable());
			UI.getCurrent().getElement().setText("Error");
		});
	}
}
