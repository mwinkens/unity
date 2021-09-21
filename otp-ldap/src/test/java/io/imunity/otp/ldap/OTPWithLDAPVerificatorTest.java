/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp.ldap;

import static io.imunity.otp.ldap.OTPWithLDAPProperties.PREFIX;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.ldap.client.config.common.LDAPCommonProperties.PORTS;
import static pl.edu.icm.unity.ldap.client.config.common.LDAPCommonProperties.SERVERS;
import static pl.edu.icm.unity.ldap.client.config.common.LDAPCommonProperties.USER_DN_TEMPLATE;
import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;

import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import io.imunity.otp.HashFunction;
import io.imunity.otp.OTPGenerationParams;
import io.imunity.otp.TOTPCodeGenerator;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.ldap.EmbeddedDirectoryServer;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Identity;

@RunWith(MockitoJUnitRunner.class)
public class OTPWithLDAPVerificatorTest
{
	private static InMemoryDirectoryServer ds;
	private static String port;
	private static String hostname;

	private static PKIManagement pkiManagement;

	@Mock
	private IdentityResolver identityResolver;

	@Before
	public void startEmbeddedServer() throws Exception
	{
		KeystoreCredential credential = new KeystoreCredential("src/test/resources/pki/demo-localhost.p12",
				"the!unity".toCharArray(), "the!unity".toCharArray(), "unity-demo", "PKCS12");
		EmbeddedDirectoryServer embeddedDirectoryServer = new EmbeddedDirectoryServer(credential, "src/test/resources",
				ServerHostnameCheckingMode.WARN);
		ds = embeddedDirectoryServer.startEmbeddedServer();
		hostname = embeddedDirectoryServer.getPlainConnection().getConnectedAddress();
		port = embeddedDirectoryServer.getPlainConnection().getConnectedPort() + "";
		pkiManagement = embeddedDirectoryServer.getPKIManagement4Client();
	}

	@After
	public void shutdown()
	{
		ds.shutDown(true);
	}

	@Test
	public void shouldVerifyUsingSecretFromLdap() throws IOException, IllegalIdentityValueException,
			IllegalTypeException, IllegalGroupValueException, EngineException
	{
		Properties p = new Properties();

		p.setProperty(PREFIX + SERVERS + "1", hostname);
		p.setProperty(PREFIX + PORTS + "1", port);
		p.setProperty(PREFIX + USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX + OTPWithLDAPProperties.SYSTEM_DN, "cn=user1,ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX + OTPWithLDAPProperties.SYSTEM_PASSWORD, "user1");
		p.setProperty(PREFIX + OTPWithLDAPProperties.OTP_SECRET_ATTRIBUTE, "sn");
		p.setProperty(PREFIX + OTPWithLDAPProperties.OTP_ALLOWED_TIME_DRIFT_STEPS, "3");
		p.setProperty(PREFIX + OTPWithLDAPProperties.OTP_TIME_STEP_SECODS, "40");
		p.setProperty(PREFIX + OTPWithLDAPProperties.OTP_HASH_FUNCTION, HashFunction.SHA1.toString());

		OTPWithLDAPProperties lp = new OTPWithLDAPProperties(p);

		OTPWithLDAPVerificator verificator = new OTPWithLDAPVerificator(pkiManagement);
		verificator.setSerializedConfiguration(getConfigAsString(lp));
		AuthenticationSubject subject = AuthenticationSubject.entityBased(1);
		verificator.setIdentityResolver(identityResolver);
		long currentTime = System.currentTimeMillis();
		String correctCode = TOTPCodeGenerator.generateTOTP("JBSWY3DPEHPK3PXP", currentTime / 1000,
				new OTPGenerationParams(6, HashFunction.SHA1, 40));
		when(identityResolver.resolveSubject(subject, UsernameIdentity.ID))
				.thenReturn(new Identity(UsernameIdentity.ID, "user1", 1l, ""));

		AuthenticationResult result = verificator.verifyCode(correctCode, AuthenticationSubject.entityBased(1));
		assertThat(result.getStatus(), is(Status.success));
	}

	private String getConfigAsString(OTPWithLDAPProperties otpWithLDAPProperties) throws IOException
	{
		StringWriter sbw = new StringWriter();
		otpWithLDAPProperties.getProperties().store(sbw, "");
		return sbw.toString();
	}

}
