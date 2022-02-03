/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import org.springframework.stereotype.Component;

import io.imunity.scim.common.BasicSCIMResource;
import io.imunity.scim.common.ListResponse;
import io.imunity.scim.common.Meta;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.group.SCIMGroupRestController;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

class SCIMUserResourceAssemblyService
{
	private static final String DEFAULT_META_VERSION = "v1";
	private final SCIMEndpointDescription configuration;

	SCIMUserResourceAssemblyService(SCIMEndpointDescription configuration)
	{
		this.configuration = configuration;
	}

	BasicSCIMResource mapToUserResource(User user)
	{
		return mapToSingleUserResource(user);
	}

	ListResponse<SCIMUserResource> mapToListUsersResource(List<User> users)
	{
		List<SCIMUserResource> usersResource = users.stream().map(u -> mapToSingleUserResource(u))
				.collect(Collectors.toList());
		return ListResponse.<SCIMUserResource>builder().withResources(usersResource)
				.withTotalResults(usersResource.size()).build();
	}

	SCIMUserResource mapToSingleUserResource(User user)
	{
		UserIdentity persistence = user.identities.stream().filter(i -> i.typeId.equals(PersistentIdentity.ID))
				.findFirst().get();
		Instant lastModified = user.identities.stream().map(i -> i.updateTs).sorted(Comparator.reverseOrder())
				.findFirst().get();

		URI location = UriBuilder.fromUri(configuration.baseLocation)
				.path(SCIMUserRestController.SINGLE_USER_LOCATION + "/" + persistence.value).build();

		return SCIMUserResource.builder().withId(persistence.value)
				.withMeta(Meta.builder().withResourceType(Meta.ResourceType.User).withVersion(DEFAULT_META_VERSION)
						.withCreated(persistence.creationTs).withLastModified(lastModified).withLocation(location)
						.build())
				.withUserName(getUserNameFallbackToNone(user.identities))
				.withGroups(user.groups.stream()
						.map(g -> SCIMUserGroupResource.builder().withDisplay(g.displayName)
								.withRef(getGroupLocation(g)).withValue(g.value).build())
						.collect(Collectors.toList()))
				.build();
	}

	private URI getGroupLocation(UserGroup group)
	{
		return UriBuilder.fromUri(configuration.baseLocation).path(SCIMGroupRestController.SINGLE_GROUP_LOCATION)
				.path(URLEncoder.encode(group.value, StandardCharsets.UTF_8)).build();
	}

	private String getUserNameFallbackToNone(List<UserIdentity> identities)
	{
		Optional<UserIdentity> userNameId = identities.stream().filter(i -> i.typeId.equals(UsernameIdentity.ID))
				.findFirst();
		if (userNameId.isPresent())
			return userNameId.get().value;
		Optional<UserIdentity> emailId = identities.stream().filter(i -> i.typeId.equals(EmailIdentity.ID)).findFirst();
		if (emailId.isPresent())
			return emailId.get().value;

		return "none";
	}

	@Component
	static class SCIMUserAssemblyServiceFactory
	{
		SCIMUserResourceAssemblyService getService(SCIMEndpointDescription configuration)
		{
			return new SCIMUserResourceAssemblyService(configuration);
		}
	}
}
