/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.util.Arrays;
import java.util.Optional;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.oauth.as.OAuthClient;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.OAuthTokenRepository;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Implementation of RFC 7009  https://tools.ietf.org/html/rfc7009
 * <p>
 * Limitations: 
 * The endpoint access is not authorized - or better said the access
 * is authorized implicitly by providing a valid access token to be revoked. The client_id must be always given. 
 * <p>
 * Typical usage:
 * <code>
 * POST /.../revoke HTTP/1.1
   Host: ... 
   Content-Type: application/x-www-form-urlencoded
   
   token=45ghiukldjahdnhzdauz&client_id=oauth-client&token_type_hint=refresh_token
 * </code>
 * <p>
 * Unity also supports one non standard extension. If a logout=true parameter is added, then 
 * besides token revocation also the token's owner's session is killed. To allow for this,
 * a special OAuth scope must be associated with the token: 'single-logout'.
 * 
 * 
 * @author K. Benedyczak
 */
@Produces("application/json")
@Path(OAuthTokenEndpoint.TOKEN_REVOCATION_PATH)
public class RevocationResource extends BaseOAuthResource
{
	public static final String TOKEN_TYPE = "token_type_hint";
	public static final String TOKEN_TYPE_ACCESS = "access_token";
	public static final String TOKEN_TYPE_REFRESH = "refresh_token";
	public static final String UNSUPPORTED_TOKEN_TYPE_ERROR = "unsupported_token_type";
	public static final String TOKEN = "token";
	public static final String CLIENT = "client_id";
	public static final String LOGOUT = "logout";
	public static final String LOGOUT_SCOPE = "single-logout";
	
	private final TokensManagement tokensManagement;
	private final SessionManagement sessionManagement;
	private final AuthenticationRealm realm;
	private final OAuthTokenRepository oauthTokenRepository;
	private final String clientsGroup;
	
	public RevocationResource(TokensManagement tokensManagement, OAuthTokenRepository oauthTokenRepository,
			SessionManagement sessionManagement, 
			AuthenticationRealm realm,
			String clientsGroup,
			)
	{
		this.tokensManagement = tokensManagement;
		this.oauthTokenRepository = oauthTokenRepository;
		this.sessionManagement = sessionManagement;
		this.realm = realm;
		this.clientsGroup = clientsGroup;
	}

	@Path("/")
	@POST
	public Response revoke(@FormParam(TOKEN) String token, @FormParam(CLIENT) String clientId, 
			@FormParam(TOKEN_TYPE) String tokenHint, @FormParam(LOGOUT) String logout) 
			throws EngineException, JsonProcessingException
	{
		if (token == null)
			return makeError(OAuth2Error.INVALID_REQUEST, "To access the token revocation endpoint "
					+ "a token must be provided");
		
		if (tokenHint != null && !TOKEN_TYPE_ACCESS.equals(tokenHint) && !TOKEN_TYPE_REFRESH.equals(tokenHint))
			return makeError(new ErrorObject(UNSUPPORTED_TOKEN_TYPE_ERROR, "Invalid request", 
					HTTPResponse.SC_BAD_REQUEST), 
					"Token type '" + tokenHint + "' is not supported");
		
		Token internalToken;
		try
		{
			internalToken = loadToken(token, tokenHint);
		} catch (IllegalArgumentException e)
		{
			return toResponse(Response.ok());
		}
		
		OAuthToken parsedToken = parseInternalToken(internalToken);
		
		if (clientId != null && !clientId.equals(parsedToken.getClientUsername()))
			return makeError(OAuth2Error.INVALID_CLIENT, "Wrong client/token");
		
		ClientType clientType = parsedToken.getClientType() == null ? ClientType.CONFIDENTIAL : parsedToken.getClientType();
		if (clientType == ClientType.PUBLIC)
		{
			if (clientId == null)
				return makeError(OAuth2Error.INVALID_REQUEST, "To access the token revocation endpoint "
						+ "a " + CLIENT + " must be provided");
		} else
		{
			//TODO ensure request is authenticated
		}
		
		
		if ("true".equals(logout))
		{
			Response r = killSession(parsedToken, internalToken.getOwner());
			if (r != null)
				return r;
		}
		
		try
		{
			tokensManagement.removeToken(internalToken.getType(), token);
		} catch (IllegalArgumentException e)
		{
			//ok
		}
		return toResponse(Response.ok());
	}

	private Token loadToken(String token, String tokenHint)
	{
		if (TOKEN_TYPE_ACCESS.equals(tokenHint))
		{
			return oauthTokenRepository.readAccessToken(token);
		} else if (TOKEN_TYPE_REFRESH.equals(tokenHint))
		{
			return tokensManagement.getTokenById(OAuthProcessor.INTERNAL_REFRESH_TOKEN, token);
		} else
		{
			try
			{
				return oauthTokenRepository.readAccessToken(token);
			} catch (IllegalArgumentException notFound)
			{
				return tokensManagement.getTokenById(OAuthProcessor.INTERNAL_REFRESH_TOKEN, token);
			}
		}
	}
	
	private Response killSession(OAuthToken parsedAccessToken, long entity) throws EngineException
	{
		if (parsedAccessToken.getEffectiveScope() == null)
			return makeError(OAuth2Error.INVALID_SCOPE, "Insufficent scope to perform full logout.");
		Optional<String> logoutScope = Arrays.stream(parsedAccessToken.getEffectiveScope()).
				filter(scope -> LOGOUT_SCOPE.equals(scope)).
				findAny();
		if (!logoutScope.isPresent())
			return makeError(OAuth2Error.INVALID_SCOPE, "Insufficent scope to perform full logout.");
		try
		{
			LoginSession ownedSession = sessionManagement.getOwnedSession(
					new EntityParam(entity), realm.getName());
			sessionManagement.removeSession(ownedSession.getId(), true);
		} catch (WrongArgumentException e)
		{
			//ok - no session
		}
		return null;
	}
}
