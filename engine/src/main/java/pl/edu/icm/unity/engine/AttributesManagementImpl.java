/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.registries.AttributeValueTypesRegistry;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Implements attributes operations.
 * @author K. Benedyczak
 */
@Component
public class AttributesManagementImpl implements AttributesManagement
{
	private AttributeValueTypesRegistry attrValueTypesReg;
	private DBSessionManager db;
	private DBAttributes dbAttributes;
	private IdentitiesResolver idResolver;
	private AuthorizationManager authz;
	
	@Autowired
	public AttributesManagementImpl(AttributeValueTypesRegistry attrValueTypesReg,
			DBSessionManager db, DBAttributes dbAttributes,
			IdentitiesResolver idResolver, AuthorizationManager authz)
	{
		this.attrValueTypesReg = attrValueTypesReg;
		this.db = db;
		this.dbAttributes = dbAttributes;
		this.idResolver = idResolver;
		this.authz = authz;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getSupportedAttributeValueTypes() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		Collection<AttributeValueSyntax<?>> all = attrValueTypesReg.getAll();
		String[] ret = new String[all.size()];
		Iterator<AttributeValueSyntax<?>> it = all.iterator();
		for (int i=0; it.hasNext(); i++)
			ret[i] = it.next().getValueSyntaxId();
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addAttributeType(AttributeType toAdd) throws EngineException
	{
		toAdd.validateInitialization();
		if (toAdd.getFlags() != 0)
			throw new IllegalAttributeTypeException("Custom attribute types must not have any flags set");
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			dbAttributes.addAttributeType(toAdd, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateAttributeType(AttributeType at) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeAttributeType(String id, boolean deleteInstances) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			AttributeType at = dbAttributes.getAttributeType(id, sql);
			if ((at.getFlags() & (AttributeType.TYPE_IMMUTABLE_FLAG | 
					AttributeType.INSTANCES_IMMUTABLE_FLAG)) != 0)
				throw new IllegalAttributeTypeException("The attribute type with name " + id + 
						" can not be manually removed");

			dbAttributes.removeAttributeType(id, deleteInstances, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<AttributeType> getAttributeTypes() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		SqlSession sql = db.getSqlSession(false);
		try
		{
			return dbAttributes.getAttributeTypes(sql);
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addAttributeClass(AttributesClass clazz) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeAttributeClass(String id) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void assignAttributeClasses(EntityParam entity, String[] classes)
			throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> void setAttribute(EntityParam entity, Attribute<T> attribute, boolean update)
			throws EngineException
	{
		attribute.validateInitialization();
		entity.validateInitialization();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sql);
			AttributeType at = dbAttributes.getAttributeType(attribute.getName(), sql);
			authz.checkAuthorization(at.isSelfModificable() && authz.isSelf(entityId), 
					attribute.getGroupPath(), AuthzCapability.attributeModify);
			dbAttributes.addAttribute(entityId, attribute, update, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeAttribute(EntityParam entity, String groupPath, String attributeTypeId)
			throws EngineException
	{
		if (groupPath == null)
			throw new IllegalGroupValueException("Group must not be null");
		if (attributeTypeId == null)
			throw new IllegalAttributeValueException("Attribute name must not be null");
		entity.validateInitialization();

		SqlSession sql = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sql);
			AttributeType at = dbAttributes.getAttributeType(attributeTypeId, sql);
			authz.checkAuthorization(at.isSelfModificable() && authz.isSelf(entityId),
					groupPath, AuthzCapability.attributeModify);
			dbAttributes.removeAttribute(entityId, groupPath, attributeTypeId, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<Attribute<?>> getAttributes(EntityParam entity, String groupPath,
			String attributeTypeId) throws EngineException
	{
		Collection<Attribute<?>> ret = getAllAttributesInternal(entity, groupPath, attributeTypeId, 
				AuthzCapability.read);
		filterLocal(ret);
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<Attribute<?>> getAllAttributes(EntityParam entity, String groupPath,
			String attributeTypeId) throws EngineException
	{
		return getAllAttributesInternal(entity, groupPath, attributeTypeId, AuthzCapability.attributeModify);
	}

	private void filterLocal(Collection<Attribute<?>> unfiltered)
	{
		Iterator<Attribute<?>> it = unfiltered.iterator();
		while (it.hasNext())
		{
			Attribute<?> attr = it.next();
			if (attr.getVisibility() == AttributeVisibility.local)
				it.remove();
		}
	}
	
	private Collection<Attribute<?>> getAllAttributesInternal(EntityParam entity, String groupPath,
			String attributeTypeName, AuthzCapability requiredCapability) throws EngineException
	{
		entity.validateInitialization();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sql);
			authz.checkAuthorization(authz.isSelf(entityId), groupPath, requiredCapability);
			Collection<Attribute<?>> ret = dbAttributes.getAllAttributes(entityId, groupPath, 
					attributeTypeName, sql);
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}	
	}
}
