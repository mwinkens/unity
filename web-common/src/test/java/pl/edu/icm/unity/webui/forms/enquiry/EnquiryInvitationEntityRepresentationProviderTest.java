/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms.enquiry;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Test;

import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

public class EnquiryInvitationEntityRepresentationProviderTest
{

	@Test
	public void shouldUseOnlyRemoteIds() throws Exception
	{
		EnquiryInvitationEntityRepresentationProvider repProvider = new EnquiryInvitationEntityRepresentationProvider(
				l -> Optional.of("disp" + l));

		Entity entity = new Entity(Arrays.asList(
				new Identity(new IdentityParam(EmailIdentity.ID, "remote@test.com", "remoteIdp1", "tp"), 1L,
						"remote@test.com"),
				new Identity(new IdentityParam(X500Identity.ID, "X500", "remoteIdp2", "tp"), 1L, "X500"),
				new Identity(new IdentityParam(EmailIdentity.ID, "local@test.com", null, null), 1L, "local@test.com")

		), new EntityInformation(1L), null);

		String rep = repProvider.getEntityRepresentation(entity);
		assertThat(rep, is("disp1: remoteIdp1 & remoteIdp2"));

	}

	@Test
	public void shouldUseOnlyRemoteIdsAsHostname() throws Exception
	{
		EnquiryInvitationEntityRepresentationProvider repProvider = new EnquiryInvitationEntityRepresentationProvider(
				l -> Optional.of("disp" + l));

		Entity entity = new Entity(Arrays.asList(
				new Identity(new IdentityParam(EmailIdentity.ID, "remote@test.com", "https://account.google.com", "tp"),
						1L, "remote@test.com"),
				new Identity(new IdentityParam(X500Identity.ID, "X500", "remoteIdp2", "tp"), 1L, "X500")

		), new EntityInformation(1L), null);

		String rep = repProvider.getEntityRepresentation(entity);
		assertThat(rep, is("disp1: account.google.com & remoteIdp2"));

	}

	@Test
	public void shouldUseLocalIds() throws Exception
	{
		EnquiryInvitationEntityRepresentationProvider repProvider = new EnquiryInvitationEntityRepresentationProvider(
				l -> Optional.of("disp" + l));

		Entity entity = new Entity(Arrays.asList(
				new Identity(new IdentityParam(EmailIdentity.ID, "local@test.com", null, null), 1L, "remote@test.com"),
				new Identity(new IdentityParam(X500Identity.ID, "X500", null, null), 1L, "X500")

		), new EntityInformation(1L), null);

		String rep = repProvider.getEntityRepresentation(entity);
		assertThat(rep, is("disp1: local@test.com & X500"));
	}

	@Test
	public void shouldSkipAnonymousIdentities() throws Exception
	{
		EnquiryInvitationEntityRepresentationProvider repProvider = new EnquiryInvitationEntityRepresentationProvider(
				l -> Optional.of("disp" + l));

		Entity entity = new Entity(
				Arrays.asList(new Identity(new IdentityParam(IdentifierIdentity.ID, "id", null, null), 1L, "id"),
						new Identity(new IdentityParam(PersistentIdentity.ID, "pid", null, null), 1L, "pid"),
						new Identity(new IdentityParam(EmailIdentity.ID, "local@test.com", null, null), 1L,
								"remote@test.com"),
						new Identity(new IdentityParam(X500Identity.ID, "X500", null, null), 1L, "X500")

				), new EntityInformation(1L), null);

		String rep = repProvider.getEntityRepresentation(entity);
		assertThat(rep, is("disp1: local@test.com & X500"));
	}

	@Test
	public void shouldSkipDisplayedName() throws Exception
	{
		EnquiryInvitationEntityRepresentationProvider repProvider = new EnquiryInvitationEntityRepresentationProvider(
				l -> Optional.empty());

		Entity entity = new Entity(Arrays.asList(
				new Identity(new IdentityParam(IdentifierIdentity.ID, "id", null, null), 1L, "id"),
				new Identity(new IdentityParam(EmailIdentity.ID, "local@test.com", null, null), 1L, "remote@test.com")

		), new EntityInformation(1L), null);

		String rep = repProvider.getEntityRepresentation(entity);
		assertThat(rep, is("local@test.com"));
	}

}
