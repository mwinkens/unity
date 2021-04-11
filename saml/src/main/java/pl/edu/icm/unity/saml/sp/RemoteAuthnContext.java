/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.io.Serializable;

import eu.unicore.samly2.messages.SAMLVerifiableElement;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnState;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;


/**
 * Context of a remote SAML authentication. Basically thread safe. As endpoint reconfiguration may occur
 * during authentication configuration valid at the beginning of authentication is stored internally. 
 * @author K. Benedyczak
 */
public class RemoteAuthnContext extends RemoteAuthnState implements Serializable
{
	private String request;
	private String requestId;
	private Binding responseBinding;
	private String response;
	private SAMLVerifiableElement verifiableResponse;
	private String returnUrl;

	private SAMLSPProperties samlProperties;
	private String idpKey;

	public RemoteAuthnContext(SAMLSPProperties config, String entryKey, AuthenticationOptionKey authnOptionKey)
	{
		super(authnOptionKey);
		this.samlProperties = config.clone();
		this.idpKey = entryKey;
	}

	public synchronized String getReturnUrl()
	{
		return returnUrl;
	}

	public synchronized String getRequest()
	{
		return request;
	}
	public synchronized void setRequest(String request, String requestId, String returnUrl)
	{
		this.request = request;
		this.requestId = requestId;
		this.returnUrl = returnUrl;
	}
	
	public synchronized void setResponse(String response, Binding responseBinding, SAMLVerifiableElement verifiableResponse)
	{
		this.response = response;
		this.responseBinding = responseBinding;
		this.verifiableResponse = verifiableResponse;
	}

	public synchronized String getIdpUrl()
	{
		return samlProperties.getValue(idpKey + SAMLSPProperties.IDP_ADDRESS);
	}
	public synchronized String getResponse()
	{
		return response;
	}
	public synchronized Binding getRequestBinding()
	{
		return samlProperties.getEnumValue(idpKey + SAMLSPProperties.IDP_BINDING, 
				Binding.class);
	}
	public synchronized Binding getResponseBinding()
	{
		return responseBinding;
	}
	public synchronized String getRequestId()
	{
		return requestId;
	}
	public synchronized String getGroupAttribute()
	{
		return samlProperties.getValue(idpKey + SAMLSPProperties.IDP_GROUP_MEMBERSHIP_ATTRIBUTE);
	}
	public synchronized String getRegistrationFormForUnknown()
	{
		return samlProperties.getValue(
				idpKey + CommonWebAuthnProperties.REGISTRATION_FORM);
	}
	public synchronized boolean isEnableAssociation()
	{
		String perIdpKey = idpKey + CommonWebAuthnProperties.ENABLE_ASSOCIATION;
		return samlProperties.isSet(perIdpKey) ? 
				samlProperties.getBooleanValue(perIdpKey) :
				samlProperties.getBooleanValue(CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION);
	}
	
	public synchronized SAMLSPProperties getContextConfig()
	{
		return samlProperties;
	}
	
	public synchronized String getContextIdpKey()
	{
		return idpKey;
	}
	
	public synchronized SAMLVerifiableElement getVerifiableResponse()
	{
		return verifiableResponse;
	}
}
