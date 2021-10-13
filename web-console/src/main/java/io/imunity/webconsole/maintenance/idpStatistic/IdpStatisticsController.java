/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.maintenance.idpStatistic;

import java.util.Collection;
import java.util.Date;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement.GroupBy;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@Component
class IdpStatisticsController
{

	private final IdpStatisticManagement idpStatisticManagement;
	private final MessageSource msg;

	IdpStatisticsController(IdpStatisticManagement idpStatisticManagement, MessageSource msg)
	{
		this.msg = msg;
		this.idpStatisticManagement = idpStatisticManagement;
	}

	Collection<GroupedIdpStatistic> getIdpStatistics(Date since) throws ControllerException
	{
		try
		{
			return idpStatisticManagement.getIdpStatisticsSinceGroupBy(since, GroupBy.total);
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage("IdpStatisticsController.getError"), e);
		}

	}

	void drop(Date since) throws ControllerException
	{
		try
		{
			idpStatisticManagement.deleteOlderThan(since);

		} catch (Exception er)
		{
			throw new ControllerException(msg.getMessage("IdpStatisticsController.cannotDropStatistics"), er);
		}
	}
}
