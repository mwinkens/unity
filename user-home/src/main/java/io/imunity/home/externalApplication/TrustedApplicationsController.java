/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.externalApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.idp.AccessProtocol;
import io.imunity.idp.ApplicationId;
import io.imunity.idp.IdPClientData;
import io.imunity.idp.TrustedIdPClientsManagement;
import io.imunity.idp.IdPClientData.AccessStatus;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@Component
public class TrustedApplicationsController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, TrustedApplicationsController.class);

	private final Map<AccessProtocol, TrustedIdPClientsManagement> providers;
	private final MessageSource msg;

	@Autowired
	public TrustedApplicationsController(List<TrustedIdPClientsManagement> providers, MessageSource msg)
	{
		this.providers = providers.stream().collect(Collectors.toMap(p -> p.getSupportedProtocol(), p -> p));
		this.msg = msg;
	}

	List<IdPClientData> getApplications() throws ControllerException
	{
		List<IdPClientData> applications = new ArrayList<>();
		for (TrustedIdPClientsManagement p : providers.values().stream()
				.sorted((p1, p2) -> p1.getSupportedProtocol().compareTo(p2.getSupportedProtocol()))
				.collect(Collectors.toList()))
		{
			try
			{
				applications.addAll(p.getIdpClientsData());
			} catch (EngineException e)
			{
				log.error("Can not get trusted applications", e);
				throw new ControllerException(msg.getMessage("TrustedApplicationsController.cannotGetApplications"), e);
			}
		}
		return applications;
	}

	List<IdPClientData> filterAllowedApplications(List<IdPClientData> applications)
	{
		return applications.stream().filter(a -> a.accessStatus.equals(AccessStatus.allowWithoutAsking)
				|| a.accessStatus.equals(AccessStatus.allow)).collect(Collectors.toList());
	}

	List<IdPClientData> filterDisallowedApplications(List<IdPClientData> applications)
	{
		return applications.stream().filter(a -> a.accessStatus.equals(AccessStatus.disallowWithoutAsking))
				.collect(Collectors.toList());
	}

	void ublockAccess(ApplicationId appId, AccessProtocol accessProtocol) throws ControllerException
	{
		try
		{
			providers.get(accessProtocol).unblockAccess(appId);
		} catch (EngineException e)
		{
			log.error("Can not unblock access for application " + appId, e);
			throw new ControllerException(msg.getMessage("TrustedApplicationsController.cannotUnblockAccess", appId),
					e);
		}
	}

	void revokeAccess(ApplicationId appId, AccessProtocol accessProtocol) throws ControllerException
	{
		try
		{
			providers.get(accessProtocol).revokeAccess(appId);
		} catch (EngineException e)
		{
			log.error("Can not revoke access for application " + appId, e);
			throw new ControllerException(msg.getMessage("TrustedApplicationsController.cannotRevokeAccess", appId), e);
		}
	}

}
