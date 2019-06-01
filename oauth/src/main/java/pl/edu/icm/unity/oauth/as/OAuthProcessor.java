/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import com.nimbusds.openid.connect.sdk.claims.ClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.OAuthToken.PKCSInfo;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Groups OAuth related logic for processing the request and preparing the response.  
 * @author K. Benedyczak
 */
public class OAuthProcessor
{
	public static final String INTERNAL_CODE_TOKEN = "oauth2Code";
	public static final String INTERNAL_ACCESS_TOKEN = "oauth2Access";
	public static final String INTERNAL_REFRESH_TOKEN = "oauth2Refresh";
	
	/**
	 * Returns only requested attributes for which we have mapping.
	 */
	public Set<DynamicAttribute> filterAttributes(TranslationResult userInfo, 
			Set<String> requestedAttributes)
	{
		Set<DynamicAttribute> ret = filterNotRequestedAttributes(userInfo, requestedAttributes);
		return filterUnsupportedAttributes(ret);
	}

	/**
	 * Returns Authorization response to be returned and records (if needed) 
	 * the internal state token, which is needed to associate further use of the code and/or id tokens with
	 * the authorization that currently takes place.
	 */
	public AuthorizationSuccessResponse prepareAuthzResponseAndRecordInternalState(Collection<DynamicAttribute> attributes, 
			IdentityParam identity,	OAuthAuthzContext ctx, TokensManagement tokensMan) 
					throws EngineException, JsonProcessingException, ParseException, JOSEException
	{
		OAuthToken internalToken = new OAuthToken();
		OAuthASProperties config = ctx.getConfig();
		internalToken.setEffectiveScope(ctx.getEffectiveRequestedScopesList());
		internalToken.setRequestedScope(ctx.getRequestedScopes().stream().toArray(String[]::new));
		internalToken.setClientId(ctx.getClientEntityId());
		internalToken.setRedirectUri(ctx.getReturnURI().toASCIIString());
		internalToken.setClientName(ctx.getClientName());
		internalToken.setClientUsername(ctx.getClientUsername());
		internalToken.setSubject(identity.getValue());
		internalToken.setMaxExtendedValidity(config.getMaxExtendedAccessTokenValidity());
		internalToken.setTokenValidity(config.getAccessTokenValidity()); 
		internalToken.setAudience(ctx.getClientUsername());
		internalToken.setIssuerUri(config.getIssuerName());
		internalToken.setClientType(ctx.getClientType());

		String codeChallenge = ctx.getRequest().getCodeChallenge() == null ? 
				null : ctx.getRequest().getCodeChallenge().getValue();
		String codeChallengeMethod = ctx.getRequest().getCodeChallengeMethod() == null ? 
				null : ctx.getRequest().getCodeChallengeMethod().getValue();
		PKCSInfo pkcsInfo = new PKCSInfo(codeChallenge, codeChallengeMethod);
		internalToken.setPkcsInfo(pkcsInfo);
	
		Date now = new Date();
		
		JWT idTokenSigned = null;
		ResponseType responseType = ctx.getRequest().getResponseType();
		internalToken.setResponseType(responseType.toString());
		
		UserInfo userInfo = prepareUserInfoClaimSet(identity.getValue(), attributes);
		internalToken.setUserInfo(userInfo.toJSONObject().toJSONString());

		if (ctx.isOpenIdMode())
		{
			IDTokenClaimsSet idToken = prepareIdInfoClaimSet(identity.getValue(), 
					internalToken.getAudience(), ctx, userInfo, now);
			idTokenSigned = config.getTokenSigner().sign(idToken);	
			internalToken.setOpenidToken(idTokenSigned.serialize());
			//we record OpenID token in internal state always in open id mode. However it may happen
			//that it is not requested immediately now:
			if (!responseType.contains(OIDCResponseTypeValue.ID_TOKEN))
				idTokenSigned = null;
		}
		
		AuthorizationSuccessResponse oauthResponse = null;
		if (GrantFlow.authorizationCode == ctx.getFlow())
		{
			AuthorizationCode authzCode = new AuthorizationCode();
			internalToken.setAuthzCode(authzCode.getValue());
			oauthResponse = new AuthorizationSuccessResponse(ctx.getReturnURI(), authzCode, null,
					ctx.getRequest().getState(), ctx.getRequest().impliedResponseMode());
			Date expiration = new Date(now.getTime() + config.getCodeTokenValidity() * 1000);
			tokensMan.addToken(INTERNAL_CODE_TOKEN, authzCode.getValue(), 
					new EntityParam(identity), internalToken.getSerialized(), now, expiration);
		} else if (GrantFlow.implicit == ctx.getFlow())
		{
			if (responseType.contains(OIDCResponseTypeValue.ID_TOKEN) && responseType.size() == 1)
			{
				//we return only the id token, no access token so we don't need an internal token.
				return new AuthenticationSuccessResponse(
						ctx.getReturnURI(), null, idTokenSigned, 
						null, ctx.getRequest().getState(), null, 
						ctx.getRequest().impliedResponseMode());
			}

			AccessToken accessToken = createAccessToken(ctx);
			internalToken.setAccessToken(accessToken.getValue());
			Date expiration = new Date(now.getTime() + config.getAccessTokenValidity() * 1000);
			oauthResponse = new AuthenticationSuccessResponse(
						ctx.getReturnURI(), null, idTokenSigned, 
						accessToken, ctx.getRequest().getState(), null, 
						ctx.getRequest().impliedResponseMode());
			tokensMan.addToken(INTERNAL_ACCESS_TOKEN, accessToken.getValue(), 
					new EntityParam(identity), internalToken.getSerialized(), now, expiration);
		} else if (GrantFlow.openidHybrid == ctx.getFlow())
		{
			//in hybrid mode authz code is returned always
			AuthorizationCode authzCode = new AuthorizationCode();
			internalToken.setAuthzCode(authzCode.getValue());
			Date codeExpiration = new Date(now.getTime() + config.getCodeTokenValidity() * 1000);
			tokensMan.addToken(INTERNAL_CODE_TOKEN, authzCode.getValue(), 
					new EntityParam(identity), internalToken.getSerialized(), 
					now, codeExpiration);
			
			//access token - sometimes
			AccessToken accessToken = null;
			if (responseType.contains(ResponseType.Value.TOKEN))
			{
				accessToken = createAccessToken(ctx);
				internalToken.setAccessToken(accessToken.getValue());
				Date accessExpiration = new Date(now.getTime() + config.getAccessTokenValidity() * 1000);
				tokensMan.addToken(INTERNAL_ACCESS_TOKEN, accessToken.getValue(), 
						new EntityParam(identity), internalToken.getSerialized(), 
						now, accessExpiration);
			}
			
			//id token also sometimes, but this was handled before.
			oauthResponse = new AuthenticationSuccessResponse(
					ctx.getReturnURI(), authzCode, idTokenSigned, 
					accessToken, ctx.getRequest().getState(), null, 
					ctx.getRequest().impliedResponseMode());
		}
		
		return oauthResponse;
	}
	
	
	
	/**
	 * Returns a collection of attributes including only those attributes for which there is an OAuth 
	 * representation.
	 */
	private Set<DynamicAttribute> filterUnsupportedAttributes(Set<DynamicAttribute> src)
	{
		Set<DynamicAttribute> ret = new HashSet<>();
		OAuthAttributeMapper mapper = new DefaultOAuthAttributeMapper();
		
		for (DynamicAttribute a: src)
			if (mapper.isHandled(a.getAttribute()))
				ret.add(a);
		return ret;
	}
	
	
	private Set<DynamicAttribute> filterNotRequestedAttributes(TranslationResult translationResult, 
			Set<String> requestedAttributes)
	{
		Collection<DynamicAttribute> allAttrs = translationResult.getAttributes();
		Set<DynamicAttribute> filteredAttrs = new HashSet<>();
		
		for (DynamicAttribute attr: allAttrs)
			if (requestedAttributes.contains(attr.getAttribute().getName()))
				filteredAttrs.add(attr);
		return filteredAttrs;
	}
	
	
	/**
	 * Creates an OIDC ID Token. The token includes regular attributes if and only if the access token is 
	 * not issued in the flow. This is the case if the only response type is 'id_token'. Section 5.4 of 
	 * OIDC specification.
	 */
	private IDTokenClaimsSet prepareIdInfoClaimSet(String userIdentity, String audience, OAuthAuthzContext context, 
			ClaimsSet regularAttributes, Date now)
	{
		AuthenticationRequest request = (AuthenticationRequest) context.getRequest();
		IDTokenClaimsSet idToken = new IDTokenClaimsSet(
				new Issuer(context.getConfig().getIssuerName()), 
				new Subject(userIdentity), 
				Lists.newArrayList(new Audience(audience)), 
				new Date(now.getTime() + context.getConfig().getIdTokenValidity()*1000), 
				now);
		ResponseType responseType = request.getResponseType(); 
		if (responseType.contains(OIDCResponseTypeValue.ID_TOKEN) && responseType.size() == 1)
			idToken.putAll(regularAttributes);
		if (request.getNonce() != null)
			idToken.setNonce(request.getNonce());
		return idToken;
	}
	
	public UserInfo prepareUserInfoClaimSet(String userIdentity, Collection<DynamicAttribute> attributes)
	{
		UserInfo userInfo = new UserInfo(new Subject(userIdentity));
		
		OAuthAttributeMapper mapper = new DefaultOAuthAttributeMapper();
		
		for (DynamicAttribute dat: attributes)
		{
			Attribute attr = dat.getAttribute();
			if (mapper.isHandled(attr))
			{
				String name = mapper.getJsonKey(attr);
				Object value = mapper.getJsonValue(attr);
				userInfo.setClaim(name, value);
			}
		}
		
		return userInfo;
	}
	
	/**
	 * @return a properly set up access token. It contains the effective scopes if those
	 * are different from requested.  
	 */
	public static AccessToken createAccessToken(OAuthAuthzContext ctx)
	{
		int tokenValidity = ctx.getConfig().getAccessTokenValidity();
		return new BearerAccessToken(tokenValidity, new Scope(ctx.getEffectiveRequestedScopesList()));
	}
	
	/**
	 * @return a properly set up access token. It contains the effective scopes if those
	 * are different from requested.  
	 */
	public static AccessToken createAccessToken(OAuthToken token)
	{
		int tokenValidity = token.getTokenValidity();
		return new BearerAccessToken(tokenValidity, new Scope(token.getEffectiveScope()));
	}
}
