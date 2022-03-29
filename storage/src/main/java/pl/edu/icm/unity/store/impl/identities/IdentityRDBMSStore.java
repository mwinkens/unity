/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.rdbms.GenericNamedRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.store.types.StoredIdentity;
import pl.edu.icm.unity.types.basic.Identity;


/**
 * RDBMS storage of {@link Identity} with caching
 * @author K. Benedyczak
 */
@Repository(IdentityRDBMSStore.BEAN)
public class IdentityRDBMSStore extends GenericNamedRDBMSCRUD<StoredIdentity, IdentityBean> implements IdentityDAO
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_BULK_OPS, IdentityRDBMSStore.class);

	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public IdentityRDBMSStore(IdentityJsonSerializer jsonSerializer)
	{
		super(IdentitiesMapper.class, jsonSerializer, NAME);
	}

	@Override
	public List<StoredIdentity> getByEntityFull(long entityId)
	{
		IdentitiesMapper mapper = SQLTransactionTL.getSql().getMapper(IdentitiesMapper.class);
		List<IdentityBean> allInDB = mapper.getByEntity(entityId);
		List<StoredIdentity> ret = new ArrayList<>(allInDB.size());
		for (IdentityBean bean: allInDB)
			ret.add(jsonSerializer.fromDB(bean));
		return ret;
	}
	
	@Override
	protected void preUpdateCheck(StoredIdentity oldBean, StoredIdentity supdated)
	{
		Identity old = oldBean.getIdentity();
		Identity updated = supdated.getIdentity();
		if (!old.getTypeId().equals(updated.getTypeId()))
			throw new IllegalArgumentException("Can not change identity type from " + 
					old.getTypeId() + " to " + updated.getTypeId());
		if (!old.getComparableValue().equals(updated.getComparableValue()))
			throw new IllegalArgumentException("Can not change identity value from " + 
					old.getComparableValue() + " to " + updated.getValue());
		
	}

	@Override
	public List<Identity> getByEntity(long entityId)
	{
		return getByEntityFull(entityId).stream().map(si -> si.getIdentity()).collect(Collectors.toList());
	}

	@Override
	public List<StoredIdentity> getByGroup(String group)
	{
		IdentitiesMapper mapper = SQLTransactionTL.getSql().getMapper(IdentitiesMapper.class);
		long a = System.currentTimeMillis();
		List<IdentityBean> allInDB = mapper.getByGroup(group);
		log.info("DB cost time {}", System.currentTimeMillis() - a);

		List<StoredIdentity> ret = new ArrayList<>(allInDB.size());
		a = System.currentTimeMillis();
		for (IdentityBean bean: allInDB)
			ret.add(jsonSerializer.fromDB(bean));

		log.info("Serializer cost time {}", System.currentTimeMillis() - a);
		log.info("Size {}", ret.size());

		return ret;
	}

	@Override
	public long getCountByType(List<String> types)
	{
		IdentitiesMapper mapper = SQLTransactionTL.getSql().getMapper(IdentitiesMapper.class);
		return mapper.getCountByType(types);
	}
}
