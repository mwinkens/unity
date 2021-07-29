/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

/**
 * Stores information about authenticated entity during the authentication.
 * <p>
 * This information is stored in {@link LoginSession} along with additional data after authentication is successful.  
 * @author K. Benedyczak
 */
public class AuthenticatedEntity
{
	private Long entityId;
	private String outdatedCredentialId;
	private List<String> authenticatedWith;
	private String remoteIdP;

	public AuthenticatedEntity(Long entityId, AuthenticationSubject authnSubject, String outdatedCredentialId)
	{
		this(entityId, authnSubject.identity == null ? 
				Collections.emptySet() : Collections.singleton(authnSubject.identity), 
				outdatedCredentialId);
	}
	
	public AuthenticatedEntity(Long entityId, String info, String outdatedCredentialId)
	{
		this(entityId, Sets.newHashSet(info), outdatedCredentialId);
	}

	public AuthenticatedEntity(Long entityId, Set<String> info, String outdatedCredentialId)
	{
		Preconditions.checkNotNull(entityId);
		this.entityId = entityId;
		this.authenticatedWith = new ArrayList<>(4);
		authenticatedWith.addAll(info);
		this.setOutdatedCredentialId(outdatedCredentialId);
	}
	
	/**
	 * @return null in case entity was authenticated locally, id of the remote IdP otherwise.
	 */
	public String getRemoteIdP()
	{
		return remoteIdP;
	}

	public void setRemoteIdP(String remoteIdP)
	{
		this.remoteIdP = remoteIdP;
	}

	public Long getEntityId()
	{
		return entityId;
	}

	public void setEntityId(long entityId)
	{
		this.entityId = entityId;
	}

	public List<String> getAuthenticatedWith()
	{
		return authenticatedWith;
	}

	public void setAuthenticatedWith(List<String> authenticatedWith)
	{
		this.authenticatedWith = authenticatedWith;
	}

	public String getOutdatedCredentialId()
	{
		return outdatedCredentialId;
	}

	public void setOutdatedCredentialId(String outdatedCredentialId)
	{
		this.outdatedCredentialId = outdatedCredentialId;
	}

	@Override
	public String toString()
	{
		return String.format(
				"AuthenticatedEntity [entityId=%s, outdatedCredentialId=%s, authenticatedWith=%s, remoteIdP=%s]",
				entityId, outdatedCredentialId, authenticatedWith, remoteIdP);
	}
}
