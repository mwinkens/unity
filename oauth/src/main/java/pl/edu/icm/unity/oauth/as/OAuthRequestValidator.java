/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Sets;
import com.nimbusds.oauth2.sdk.Scope;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext.ScopeInfo;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthParseServlet;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Utility class with methods useful for request checking and its mapping to unity 
 * abstraction. Used by {@link OAuthParseServlet} and {@link OAuthTokenEndpoint} in the case of client 
 * credentials grant.
 * 
 * @author K. Benedyczak
 */
public class OAuthRequestValidator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthRequestValidator.class);
	
	protected OAuthASProperties oauthConfig;
	protected EntityManagement identitiesMan;
	protected AttributesManagement attributesMan;
	
	
	public OAuthRequestValidator(OAuthASProperties oauthConfig,
			EntityManagement identitiesMan, AttributesManagement attributesMan)
	{
		this.oauthConfig = oauthConfig;
		this.identitiesMan = identitiesMan;
		this.attributesMan = attributesMan;
	}

	/**
	 * Checks if the client is a member of the configured OAuth clients group 
	 */
	public void validateGroupMembership(EntityParam clientEntity, String client)
			throws OAuthValidationException
	{
		Collection<String> groups;
		try
		{
			groups = identitiesMan.getGroups(clientEntity).keySet();
		} catch (EngineException e)
		{
			log.error("Problem retrieving groups of the OAuth client", e);
			throw new OAuthValidationException("Internal error, can not retrieve OAuth client's data");
		}
		String oauthGroup = oauthConfig.getValue(OAuthASProperties.CLIENTS_GROUP);
		if (!groups.contains(oauthGroup))
			throw new OAuthValidationException("The '" + client + "' is not authorized as OAuth client "
					+ "(not in the clients group)");
	}
	
	public Map<String, AttributeExt> getAttributesNoAuthZ(EntityParam clientEntity) throws OAuthValidationException
	{
		String oauthGroup = oauthConfig.getValue(OAuthASProperties.CLIENTS_GROUP);
		Collection<AttributeExt> attrs;
		try
		{
			attrs = attributesMan.getAllAttributes(clientEntity, true, oauthGroup, null, false);
		} catch (EngineException e)
		{
			log.error("Problem retrieving attributes of the OAuth client", e);
			throw new OAuthValidationException("Internal error, can not retrieve OAuth client's data");
		}
		Map<String, AttributeExt> ret = new HashMap<>();
		attrs.stream().forEach(a -> ret.put(a.getName(), a));
		return ret;
	}
	
	public Set<GrantFlow> getAllowedFlows(Map<String, AttributeExt> attributes)
	{
		Set<GrantFlow> allowedFlows = new HashSet<>();
		AttributeExt allowedFlowsA = attributes.get(OAuthSystemAttributesProvider.ALLOWED_FLOWS);
		if (allowedFlowsA == null)
		{
			allowedFlows.add(GrantFlow.authorizationCode);
		} else
		{
			for (Object val: allowedFlowsA.getValues())
				allowedFlows.add(GrantFlow.valueOf(val.toString()));
		}
		return allowedFlows;
	}
	
	public Optional<Set<String>> getAllowedScopes(Map<String, AttributeExt> attributes)
	{
		AttributeExt allowedScopesA = attributes.get(OAuthSystemAttributesProvider.ALLOWED_SCOPES);
		if (allowedScopesA == null)
		{
			return Optional.empty();
		} else
		{
			return Optional.of(Sets.newHashSet(allowedScopesA.getValues()));
		}
	}
	
	public List<ScopeInfo> getValidRequestedScopes(Map<String, AttributeExt> clientAttributes, Scope requestedScopes)
	{
		List<ScopeInfo> ret = new ArrayList<>();
		Set<String> scopeKeys = oauthConfig.getStructuredListKeys(OAuthASProperties.SCOPES);
		Optional<Set<String>> allowedScopes = getAllowedScopes(clientAttributes);
		
		if (requestedScopes != null)
		{
			for (String scopeKey: scopeKeys)
			{
				String scope = oauthConfig.getValue(scopeKey+OAuthASProperties.SCOPE_NAME);
				if (!allowedScopes.isEmpty() && !allowedScopes.get().contains(scope))
				{
					continue;
				}
				
				if (requestedScopes.contains(scope))
				{				
					String desc = oauthConfig.getValue(scopeKey+OAuthASProperties.SCOPE_DESCRIPTION);
					List<String> attributes = oauthConfig.getListOfValues(
							scopeKey+OAuthASProperties.SCOPE_ATTRIBUTES);
					ret.add(new ScopeInfo(scope, desc, attributes));
				}
			}
		}
		return ret;
	}
}
