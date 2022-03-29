/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.oauth.api.Scope;
import pl.edu.icm.unity.oauth.api.SystemScopeProvider;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;

@Component
public class OAuthScopesService
{
	private final SystemOAuthScopeProvidersRegistry systemScopeProvidersRegistry;

	@Autowired
	public OAuthScopesService(SystemOAuthScopeProvidersRegistry systemScopeProvidersRegistry)
	{
		this.systemScopeProvidersRegistry = systemScopeProvidersRegistry;
	}

	public List<String> getActiveScopeNames(OAuthASProperties config)
	{
		return getActiveScopes(config).stream().map(s -> s.name).collect(Collectors.toList());
	}

	public List<OAuthScope> getActiveScopes(OAuthASProperties config)
	{
		return getScopes(config).stream().filter(s -> s.enabled).collect(Collectors.toList());
	}

	public List<OAuthScope> getScopes(OAuthASProperties config)
	{
		Set<String> scopeKeys = config.getStructuredListKeys(OAuthASProperties.SCOPES);
		List<OAuthScope> scopes = new ArrayList<>();
		for (String scopeKey : scopeKeys)
		{
			scopes.add(OAuthScope.builder().withName(config.getValue(scopeKey + OAuthASProperties.SCOPE_NAME))
					.withDescription(config.getValue(scopeKey + OAuthASProperties.SCOPE_DESCRIPTION))
					.withEnabled(config.getBooleanValue(scopeKey + OAuthASProperties.SCOPE_ENABLED))
					.withAttributes(config.getListOfValues(scopeKey + OAuthASProperties.SCOPE_ATTRIBUTES)).build());

		}
		scopes.addAll(getMissingSystemScopes(config));
		return scopes;

	}

	public List<OAuthScope> getSystemScopes()
	{
		List<OAuthScope> systenScope = new ArrayList<>();

		for (SystemScopeProvider provider : systemScopeProvidersRegistry.getAll())
		{
			for (Scope scope : provider.getScopes())
			{
				systenScope.add(OAuthScope.builder().withName(scope.name).withEnabled(false)
						.withDescription(scope.description).build());
			}
		}
		return systenScope;

	}

	private List<OAuthScope> getMissingSystemScopes(OAuthASProperties config)
	{
		List<String> configured = config.getStructuredListKeys(OAuthASProperties.SCOPES).stream()
				.map(s -> config.getValue(s + OAuthASProperties.SCOPE_NAME)).collect(Collectors.toList());

		List<OAuthScope> missingSystemScope = new ArrayList<>();

		for (SystemScopeProvider provider : systemScopeProvidersRegistry.getAll())
		{
			for (Scope scope : provider.getScopes())
			{
				if (configured.contains(scope.name))
				{
					continue;
				}

				missingSystemScope.add(OAuthScope.builder().withName(scope.name)
						.withEnabled(getSystemScopeDefaultStatusForNotAdded(scope, config))
						.withDescription(scope.description).build());
			}
		}
		return missingSystemScope;

	}

	private boolean getSystemScopeDefaultStatusForNotAdded(Scope scope, OAuthASProperties config)
	{
		if (scope.name.equals(OAuthSystemScopeProvider.OPENID_SCOPE))
		{
			return false;

		} else if (scope.name.equals(OAuthSystemScopeProvider.OFFLINE_ACCESS_SCOPE))
		{
			if (config.getEnumValue(OAuthASProperties.REFRESH_TOKEN_ISSUE_POLICY, RefreshTokenIssuePolicy.class)
					.equals(OAuthASProperties.RefreshTokenIssuePolicy.OFFLINE_SCOPE_BASED))
			{
				return true;
			} else
			{
				return false;
			}

		} else
		{
			return true;
		}
	}
}
