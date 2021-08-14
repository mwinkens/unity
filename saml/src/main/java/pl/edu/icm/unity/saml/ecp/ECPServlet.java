/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import eu.unicore.samly2.validators.ReplayAttackChecker;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * ECP servlet which performs the actual ECP profile processing over PAOS binding.
 * <p>
 * The GET request is used to ask for SAML request. The POST request is used to provide SAML response 
 * and obtain a JWT token which can be subsequently used with other Unity endpoints.
 * 
 * @author K. Benedyczak
 */
public class ECPServlet extends HttpServlet
{
	private ECPStep1Handler step1Handler;
	private ECPStep2Handler step2Handler;

	public ECPServlet(SAMLECPProperties samlProperties, RemoteMetaManager metadataManager,
			ECPContextManagement samlContextManagement, 
			String myAddress, ReplayAttackChecker replayAttackChecker, 
			RemoteAuthnResultTranslator remoteAuthnProcessor,
			TokensManagement tokensMan, PKIManagement pkiManagement, EntityManagement identitiesMan,
			SessionManagement sessionMan, AuthenticationRealm realm, String address)
	{
		step1Handler = new ECPStep1Handler(metadataManager, samlContextManagement, myAddress);
		step2Handler = new ECPStep2Handler(samlProperties, metadataManager, samlContextManagement, myAddress,
				replayAttackChecker, 
				tokensMan, pkiManagement, remoteAuthnProcessor, 
				identitiesMan, sessionMan, realm, address);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		step1Handler.processECPGetRequest(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		step2Handler.processECPPostRequest(req, resp);
	}
}
