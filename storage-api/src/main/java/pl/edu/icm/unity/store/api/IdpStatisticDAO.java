/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.api;

import java.util.Date;
import java.util.List;

import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic;

public interface IdpStatisticDAO extends BasicCRUDDAO<IdpStatistic>
{
	String DAO_ID = "IdpStatisticDAO";
	String NAME = "Idp statistic";
	
	List<IdpStatistic> getIdpStatistics(final Date from, final Date until, final int limit);

	void deleteOlderThan(final Date olderThan);

}
