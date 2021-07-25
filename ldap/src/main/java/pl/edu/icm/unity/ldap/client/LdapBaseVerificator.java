/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.client;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.cert.X509Certificate;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationException;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.ldap.client.config.LdapClientConfiguration;
import pl.edu.icm.unity.ldap.client.config.LdapProperties;
import pl.edu.icm.unity.stdext.credential.NoCredentialResetImpl;
import pl.edu.icm.unity.stdext.credential.cert.CertificateExchange;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExchange;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;

/**
 * Supports {@link PasswordExchange} and verifies the password and username against a configured LDAP 
 * server. Access to remote attributes and groups is also provided.
 * 
 * @author K. Benedyczak
 */
public abstract class LdapBaseVerificator extends AbstractRemoteVerificator implements PasswordExchange, CertificateExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_LDAP, LdapBaseVerificator.class);
	private LdapProperties ldapProperties;
	private LdapClient client;
	private LdapClientConfiguration clientConfiguration;
	private PKIManagement pkiManagement;
	private TranslationProfile translationProfile;
	
	protected LdapBaseVerificator(String name, String description, 
			RemoteAuthnResultTranslator processor,
			PKIManagement pkiManagement, String exchangeId)
	{
		super(name, description, exchangeId, processor);
		this.client = new LdapClient(name);
		this.pkiManagement = pkiManagement;
	}

	@Override
	public String getSerializedConfiguration()
	{
		StringWriter sbw = new StringWriter();
		try
		{
			ldapProperties.getProperties().store(sbw, "");
		} catch (IOException e)
		{
			throw new InternalException("Can't serialize LDAP verificator configuration", e);
		}
		return sbw.toString();
	}

	@Override
	public void setSerializedConfiguration(String source)
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(source));
			ldapProperties = new LdapProperties(properties);
			translationProfile = getTranslationProfile(ldapProperties, CommonWebAuthnProperties.TRANSLATION_PROFILE,
					CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE);
			clientConfiguration = new LdapClientConfiguration(ldapProperties, pkiManagement);
	
		} catch(ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the LDAP verificator", e);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the LDAP verificator(?)", e);
		}
	}

	@Override
	public AuthenticationResult checkPassword(String username, String password, 
			String formForUnknown, boolean enableAssociation, 
			AuthenticationTriggeringContext triggeringContext) throws AuthenticationException
	{
		try
		{
			RemotelyAuthenticatedInput input = getRemotelyAuthenticatedInput(username, password);
			RemoteAuthenticationResult result = getResult(input, translationProfile, 
					triggeringContext.isSandboxTriggered(), 
					formForUnknown, enableAssociation);
			return repackIfError(result, new ResolvableError("WebPasswordRetrieval.wrongPassword"));
		} catch (Exception e)
		{
			throw e;
		}
	}
	

	private RemotelyAuthenticatedInput getRemotelyAuthenticatedInput(
			String username, String password) throws RemoteAuthenticationException
	{
		RemotelyAuthenticatedInput input = null;
		try 
		{
			input = client.bindAndSearch(username, password, clientConfiguration);
		} catch (LdapAuthenticationException e) 
		{
			log.debug("LDAP authentication failed", e);
			throw new RemoteAuthenticationException("Authentication has failed", e);
		} catch (Exception e)
		{
			throw new RemoteAuthenticationException("Problem when authenticating against the LDAP server", e);
		}
		return input;
	}	

	@Override
	public CredentialReset getCredentialResetBackend()
	{
		return new NoCredentialResetImpl();
	}

	@Override
	public AuthenticationResult checkCertificate(X509Certificate[] chain, 
			String formForUnknown, boolean enableAssociation, AuthenticationTriggeringContext triggeringContext)
	{
		try
		{
			RemotelyAuthenticatedInput input = searchRemotelyAuthenticatedInput(
					chain[0].getSubjectX500Principal().getName());
			return getResult(input, translationProfile, triggeringContext.isSandboxTriggered(), 
					formForUnknown, enableAssociation);
		} catch (Exception e)
		{
			log.debug("LDAP authentication with certificate failed", e);
			return LocalAuthenticationResult.failed(e);
		}
	}
	
	private RemotelyAuthenticatedInput searchRemotelyAuthenticatedInput(
			String dn) throws AuthenticationException, LdapAuthenticationException
	{
		RemotelyAuthenticatedInput input = null;
		try 
		{
			input = client.search(dn, clientConfiguration);
		} catch (LdapAuthenticationException e) 
		{
			log.debug("LDAP authentication failed", e);
			throw new AuthenticationException("Authentication has failed", e);
		} catch (Exception e)
		{
			throw new AuthenticationException("Problem when authenticating against the LDAP server", e);
		}
		return input;
	}
	
	@Override
	public VerificatorType getType()
	{
		return VerificatorType.Remote;
	}
}
