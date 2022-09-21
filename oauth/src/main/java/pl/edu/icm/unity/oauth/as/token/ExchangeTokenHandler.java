/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.OAuthValidationException;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator.OAuthRequestValidatorFactory;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenStatisticPublisher.OAuthTokenStatisticPublisherFactory;
import pl.edu.icm.unity.oauth.as.token.TokenUtils.TokenUtilsFactory;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

class ExchangeTokenHandler
{

	private final OAuthASProperties config;
	private final OAuthRefreshTokenRepository refreshTokensDAO;
	private final AccessTokenFactory accessTokenFactory;
	private final OAuthAccessTokenRepository accessTokensDAO;
	private final TokenUtils tokenUtils;
	private final OAuthTokenStatisticPublisher statisticPublisher;
	private final OAuthRequestValidator requestValidator;
	private final EntityManagement idMan;

	public ExchangeTokenHandler(OAuthASProperties config, OAuthRefreshTokenRepository refreshTokensDAO,
			AccessTokenFactory accessTokenFactory, OAuthAccessTokenRepository accessTokensDAO, TokenUtils tokenUtils,
			OAuthTokenStatisticPublisher statisticPublisher, OAuthRequestValidator requestValidator,
			EntityManagement idMan)
	{
		this.config = config;
		this.refreshTokensDAO = refreshTokensDAO;
		this.accessTokenFactory = accessTokenFactory;
		this.accessTokensDAO = accessTokensDAO;
		this.tokenUtils = tokenUtils;
		this.statisticPublisher = statisticPublisher;
		this.requestValidator = requestValidator;
		this.idMan = idMan;
	}

	Response handleExchangeToken(String subjectToken, String subjectTokenType, String requestedTokenType,
			String audience, String scope, String acceptHeader) throws EngineException, JsonProcessingException
	{

		long callerEntityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
		EntityParam audienceEntity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, audience));

		Token subToken = null;
		OAuthToken parsedSubjectToken = null;

		try
		{
			subToken = accessTokensDAO.readAccessToken(subjectToken);
			parsedSubjectToken = BaseOAuthResource.parseInternalToken(subToken);
		} catch (Exception e)
		{
			return BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, "wrong subject_token");
		}

		List<String> oldRequestedScopesList = Arrays.asList(parsedSubjectToken.getRequestedScope());

		try
		{
			validateExchangeRequest(subjectTokenType, requestedTokenType, audience, callerEntityId, audienceEntity,
					oldRequestedScopesList);
		} catch (OAuthErrorException e)
		{
			return e.response;
		}

		OAuthToken newToken = null;
		try
		{
			newToken = tokenUtils.prepareNewToken(parsedSubjectToken, scope, oldRequestedScopesList,
					subToken.getOwner(), callerEntityId, audience,
					requestedTokenType != null && requestedTokenType.equals(AccessTokenResource.ID_TOKEN_TYPE_ID),
					GrantType.TOKEN_EXCHANGE.getValue());
		} catch (OAuthErrorException e)
		{
			return e.response;
		}

		newToken.setClientId(callerEntityId);
		newToken.setAudience(List.of(audience));
		newToken.setClientUsername(audience);
		newToken.setClientType(parsedSubjectToken.getClientType());

		try
		{
			newToken.setClientName(tokenUtils.getClientName(audienceEntity));
		} catch (OAuthErrorException e)
		{
			return e.response;
		}

		Date now = new Date();
		AccessToken accessToken = accessTokenFactory.create(newToken, now, acceptHeader);
		newToken.setAccessToken(accessToken.getValue());

		RefreshToken refreshToken = refreshTokensDAO
				.getRefreshToken(config, now, newToken, subToken.getOwner(), Optional.empty()).orElse(null);
		Date accessExpiration = tokenUtils.getAccessTokenExpiration(config, now);

		Map<String, Object> additionalParams = new HashMap<>();
		additionalParams.put("issued_token_type", AccessTokenResource.ACCESS_TOKEN_TYPE_ID);

		AccessTokenResponse oauthResponse = tokenUtils.getAccessTokenResponse(newToken, accessToken, refreshToken,
				additionalParams);
		accessTokensDAO.storeAccessToken(accessToken, newToken, new EntityParam(subToken.getOwner()), now,
				accessExpiration);
		statisticPublisher.reportSuccess(parsedSubjectToken.getClientUsername(), parsedSubjectToken.getClientName(),
				new EntityParam(subToken.getOwner()));

		return BaseOAuthResource.toResponse(Response.ok(BaseOAuthResource.getResponseContent(oauthResponse)));
	}

	private void validateExchangeRequest(String subjectTokenType, String requestedTokenType, String audience,
			long callerEntityId, EntityParam audienceEntity, List<String> oldRequestedScopesList)
			throws OAuthErrorException
	{
		if (!subjectTokenType.equals(AccessTokenResource.ACCESS_TOKEN_TYPE_ID))
		{
			throw new OAuthErrorException(
					BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, "unsupported subject_token_type"));
		}

		if (requestedTokenType != null)
		{
			if (!(requestedTokenType.equals(AccessTokenResource.ACCESS_TOKEN_TYPE_ID)
					|| requestedTokenType.equals(AccessTokenResource.ID_TOKEN_TYPE_ID)))
			{
				throw new OAuthErrorException(
						BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, "unsupported requested_token_type"));
			}
		}

		Entity audienceResolvedEntity = null;
		try
		{
			audienceResolvedEntity = idMan.getEntity(audienceEntity);
			requestValidator.validateGroupMembership(audienceEntity, audience);

		} catch (IllegalIdentityValueException | OAuthValidationException oe)
		{
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, "wrong audience"));
		} catch (EngineException e)
		{
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.SERVER_ERROR,
					"Internal error, can not retrieve OAuth client's data"));
		}

		if (!audienceResolvedEntity.getId().equals(callerEntityId))
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, "wrong audience"));

		if (!oldRequestedScopesList.contains(AccessTokenResource.EXCHANGE_SCOPE))
		{
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_SCOPE,
					"Orginal token must have  " + AccessTokenResource.EXCHANGE_SCOPE + " scope"));
		}
	}

	@Component
	static class ExchangeTokenHandlerFactory
	{
		private final OAuthRefreshTokenRepository refreshTokensDAO;
		private final OAuthAccessTokenRepository accessTokensDAO;
		private final TokenUtilsFactory tokenUtilsFactory;
		private final OAuthTokenStatisticPublisherFactory statisticPublisherFactory;
		private final OAuthRequestValidatorFactory requestValidatorFactory;
		private final EntityManagement idMan;

		@Autowired
		ExchangeTokenHandlerFactory(OAuthRefreshTokenRepository refreshTokensDAO,
				OAuthAccessTokenRepository accessTokensDAO, TokenUtilsFactory tokenUtilsFactory,
				OAuthTokenStatisticPublisherFactory statisticPublisherFactory,
				OAuthRequestValidatorFactory requestValidatorFactory, EntityManagement idMan)
		{

			this.refreshTokensDAO = refreshTokensDAO;
			this.accessTokensDAO = accessTokensDAO;
			this.tokenUtilsFactory = tokenUtilsFactory;
			this.statisticPublisherFactory = statisticPublisherFactory;
			this.requestValidatorFactory = requestValidatorFactory;
			this.idMan = idMan;
		}

		ExchangeTokenHandler getHandler(OAuthASProperties config, ResolvedEndpoint endpoint)
		{
			return new ExchangeTokenHandler(config, refreshTokensDAO, new AccessTokenFactory(config), accessTokensDAO,
					tokenUtilsFactory.getTokenUtils(config),
					statisticPublisherFactory.getOAuthTokenStatisticPublisher(config, endpoint),
					requestValidatorFactory.getOAuthRequestValidator(config), idMan);
		}

	}
}
