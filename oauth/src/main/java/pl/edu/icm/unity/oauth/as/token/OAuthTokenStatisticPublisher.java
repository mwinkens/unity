/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token;

import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.idp.statistic.IdpStatisticEvent;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

class OAuthTokenStatisticPublisher
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthTokenStatisticPublisher.class);

	private final ApplicationEventPublisher eventPublisher;
	private final MessageSource msg;
	private final EntityManagement idMan;
	private final OAuthRequestValidator requestValidator;
	private final ResolvedEndpoint endpoint;
	private final EndpointManagement endpointMan;

	private Endpoint authzEndpoint;

	OAuthTokenStatisticPublisher(ApplicationEventPublisher eventPublisher, MessageSource msg, EntityManagement idMan,
			OAuthRequestValidator requestValidator, ResolvedEndpoint endpoint, EndpointManagement endpointMan)
	{
		this.eventPublisher = eventPublisher;
		this.msg = msg;
		this.idMan = idMan;
		this.requestValidator = requestValidator;
		this.endpoint = endpoint;
		this.endpointMan = endpointMan;
	}

	void reportFailAsLoggedClient()
	{
		LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
		if (loginSession == null)
		{
			log.error("Can not retrieve identity of the OAuth client, skippig error reporting");
			return;
		}
		
		EntityParam clientEntity = new EntityParam(loginSession.getEntityId());
		Entity clientResolvedEntity;
		try
		{
			clientResolvedEntity = idMan.getEntity(clientEntity);

		} catch (Exception e)
		{
			log.error("Can not retrieving identity of the OAuth client", e);
			return;
		}

		Identity username = clientResolvedEntity.getIdentities().stream()
				.filter(i -> i.getTypeId().equals(UsernameIdentity.ID)).findFirst().orElse(null);
		Map<String, AttributeExt> attributes;
		try
		{
			attributes = requestValidator.getAttributesNoAuthZ(clientEntity);
		} catch (Exception e)
		{
			log.error("Can not retrieving attributes of the OAuth client", e);
			return;
		}

		reportFail(username != null ? username.getComparableValue() : null,
				attributes.get(OAuthSystemAttributesProvider.CLIENT_NAME) != null
						? attributes.get(OAuthSystemAttributesProvider.CLIENT_NAME).getValues().get(0)
						: null);
	}

	void reportFail(String clientUsername, String clientName)
	{
		report(clientUsername, clientName, Status.FAILED);
	}

	void reportSuccess(String clientUsername, String clientName)
	{
		report(clientUsername, clientName, Status.SUCCESSFUL);
	}

	private void report(String clientUsername, String clientName, Status status)
	{
		Endpoint endpoint = getEndpoint();
		eventPublisher.publishEvent(new IdpStatisticEvent(endpoint.getName(),
				endpoint.getConfiguration().getDisplayedName() != null
						? endpoint.getConfiguration().getDisplayedName().getValue(msg)
						: null,
				clientUsername, clientName, status));
	}

	private Endpoint getEndpoint()
	{
		if (authzEndpoint != null)
			return authzEndpoint;

		try
		{
			Optional<Endpoint> aendpoint = endpointMan.getEndpoints().stream().filter(
					e -> e.getConfiguration().getTag().equals(endpoint.getEndpoint().getConfiguration().getTag()))
					.findFirst();
			if (aendpoint.isPresent())
			{
				authzEndpoint = aendpoint.get();
				return authzEndpoint;
			} else
			{
				return endpoint.getEndpoint();
			}
		} catch (Exception e)
		{
			log.error("Can not get relateed OAauth authz endpoint for token endpoint " + endpoint.getName(), e);
			return endpoint.getEndpoint();
		}
	}
}
