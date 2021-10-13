/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.idpStatistics;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.IdpStatisticDAO;
import pl.edu.icm.unity.store.rdbms.RDBMSDAO;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic;

@Repository(IdpStatisticsRDBMSStore.BEAN)
public class IdpStatisticsRDBMSStore implements IdpStatisticDAO, RDBMSDAO
{
	public static final String BEAN = DAO_ID + "rdbms";

	private final IdpStatisticJsonSerializer jsonSerializer;

	public IdpStatisticsRDBMSStore(IdpStatisticJsonSerializer jsonSerializer)
	{
		this.jsonSerializer = jsonSerializer;
	}

	@Override
	public void deleteOlderThan(Date olderThan)
	{
		IdpStatisticMapper mapper = SQLTransactionTL.getSql().getMapper(IdpStatisticMapper.class);
		mapper.deleteOlderThan(olderThan);
	}

	@Override
	public List<IdpStatistic> getIdpStatistics(Date from, Date until, int limit)
	{
		IdpStatisticMapper mapper = SQLTransactionTL.getSql().getMapper(IdpStatisticMapper.class);
		return mapper.getStatistics(from, until, limit).stream().map(jsonSerializer::fromDB)
				.collect(Collectors.toList());
	}

	@Override
	public long create(IdpStatistic obj)
	{
		IdpStatisticMapper mapper = SQLTransactionTL.getSql().getMapper(IdpStatisticMapper.class);
		IdpStatisticBean toAdd = jsonSerializer.toDB(obj);
		mapper.create(toAdd);
		return toAdd.getId();
	}

	@Override
	public void createWithId(long key, IdpStatistic obj)
	{
		IdpStatisticMapper mapper = SQLTransactionTL.getSql().getMapper(IdpStatisticMapper.class);
		IdpStatisticBean toAdd = jsonSerializer.toDB(obj);
		toAdd.setId(key);
		mapper.createWithKey(toAdd);

	}

	@Override
	public void updateByKey(long key, IdpStatistic obj) {
		throw new UnsupportedOperationException("Update operation is not supported for IdpStatistic.");
	}


	@Override
	public void deleteByKey(long id)
	{
		IdpStatisticMapper mapper = SQLTransactionTL.getSql().getMapper(IdpStatisticMapper.class);
		IdpStatisticBean toRemove = mapper.getByKey(id);
		if (toRemove == null)
			throw new IllegalArgumentException(NAME + " with key [" + id + "] does not exist");
		mapper.deleteByKey(id);
	}

	@Override
	public void deleteAll()
	{
		IdpStatisticMapper mapper = SQLTransactionTL.getSql().getMapper(IdpStatisticMapper.class);
		mapper.deleteAll();
	}

	@Override
	public IdpStatistic getByKey(long id)
	{
		IdpStatisticMapper mapper = SQLTransactionTL.getSql().getMapper(IdpStatisticMapper.class);
		IdpStatisticBean byName = mapper.getByKey(id);
		if (byName == null)
			throw new IllegalArgumentException(NAME + " with key [" + id + "] does not exist");
		return jsonSerializer.fromDB(byName);
	}

	@Override
	public List<IdpStatistic> getAll()
	{
		IdpStatisticMapper mapper = SQLTransactionTL.getSql().getMapper(IdpStatisticMapper.class);
		return mapper.getAll().stream().map(jsonSerializer::fromDB).collect(Collectors.toList());
	}

	@Override
	public long getCount()
	{
		IdpStatisticMapper mapper = SQLTransactionTL.getSql().getMapper(IdpStatisticMapper.class);
		return mapper.getCount();
	}
}
