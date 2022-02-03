/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.group;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.scim.config.SCIMEndpointDescription;
import pl.edu.icm.unity.engine.api.AuthorizationManagement;
import pl.edu.icm.unity.exceptions.AuthorizationException;

class SCIMGroupAuthzService
{
	private final AuthorizationManagement authzMan;
	private final SCIMEndpointDescription configuration;

	SCIMGroupAuthzService(AuthorizationManagement authzMan, SCIMEndpointDescription configuration)
	{
		this.authzMan = authzMan;
		this.configuration = configuration;
	}

	void checkReadGroups() throws AuthorizationException
	{
		authzMan.checkReadCapability(false, configuration.rootGroup);
	}

	@Component
	static class SCIMGroupAuthzServiceFactory
	{
		private final AuthorizationManagement authzMan;

		@Autowired
		SCIMGroupAuthzServiceFactory(AuthorizationManagement authzMan)
		{
			this.authzMan = authzMan;
		}

		SCIMGroupAuthzService getService(SCIMEndpointDescription configuration)
		{
			return new SCIMGroupAuthzService(authzMan, configuration);
		}
	}
}
