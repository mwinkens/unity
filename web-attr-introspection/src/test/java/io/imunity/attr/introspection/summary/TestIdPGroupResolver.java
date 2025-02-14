/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.summary;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.IdPInfo;
import pl.edu.icm.unity.types.authn.IdPInfo.IdpGroup;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

@RunWith(MockitoJUnitRunner.class)
public class TestIdPGroupResolver
{
	@Mock
	private AuthenticatorSupportService authenticatorSupportService;

	@Test
	public void shouldResolveGroupForIdps() throws EngineException
	{
		IdpGroupResolver resolver = new IdpGroupResolver(authenticatorSupportService);

		AuthenticatorInstance inst1 = Mockito.mock(AuthenticatorInstance.class);
		AuthenticatorInstance inst2 = Mockito.mock(AuthenticatorInstance.class);

		when(inst1.extractIdPs()).thenReturn(
				List.of(IdPInfo.builder().withId("idp1").withGroup(new IdpGroup("group1", Optional.empty())).build()));
		when(inst2.extractIdPs()).thenReturn(
				List.of(IdPInfo.builder().withId("idp2").withGroup(new IdpGroup("group2", Optional.empty())).build()));
		when(authenticatorSupportService.getRemoteAuthenticators(VaadinAuthentication.NAME))
				.thenReturn(List.of(inst1, inst2));
		assertThat(resolver.resolveGroupForIdp("idp1").get(), is("group1"));
		assertThat(resolver.resolveGroupForIdp("idp2").get(), is("group2"));
		assertThat(resolver.resolveGroupForIdp("unknownIdp").isEmpty(), is(true));
	}
}
