/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.impl.attribute.DBAttribute;
import pl.edu.icm.unity.store.objstore.reg.common.DBGroupSelection;
import pl.edu.icm.unity.store.objstore.reg.common.DBIdentityParam;
import pl.edu.icm.unity.store.types.common.DBConfirmationInfo;
import pl.edu.icm.unity.types.authn.ExpectedIdentity;
import pl.edu.icm.unity.types.authn.ExpectedIdentity.IdentityExpectation;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.registration.FormType;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.invite.FormPrefill;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;

public class InvitationWithCodeMapperTest extends MapperTestBase<InvitationWithCode, DBInvitationWithCode>
{

	@Override
	protected InvitationWithCode getFullAPIObject()
	{
		ObjectNode meta = Constants.MAPPER.createObjectNode();
		meta.put("1", "v");
		GroupSelection groupSelection = new GroupSelection(List.of("/g1", "/g2"));
		groupSelection.setExternalIdp("externalIdp");
		groupSelection.setTranslationProfile("Profile");
		Attribute attr = new Attribute("attr", "syntax", "/A", Lists.newArrayList("v1", "v2"), "remoteIdp",
				"translationProfile");
		ConfirmationInfo confirmationInfo = new ConfirmationInfo(true);
		confirmationInfo.setSentRequestAmount(1);
		confirmationInfo.setConfirmationDate(1L);

		IdentityParam idParam1 = new IdentityParam("email", "test@wp.pl", "remoteIdp", "Profile");
		confirmationInfo.setSentRequestAmount(1);
		idParam1.setConfirmationInfo(confirmationInfo);
		idParam1.setRealm("realm");
		idParam1.setTarget("target");
		idParam1.setMetadata(meta);

		FormPrefill formPrefill = new FormPrefill();
		formPrefill.setAllowedGroups(Map.of(1, groupSelection));
		formPrefill.setAttributes(Map.of(1, new PrefilledEntry<Attribute>(attr, PrefilledEntryMode.READ_ONLY)));
		formPrefill.setFormId("formId");
		formPrefill.setFormType(FormType.REGISTRATION);
		formPrefill.setGroupSelections(
				Map.of(1, new PrefilledEntry<GroupSelection>(groupSelection, PrefilledEntryMode.READ_ONLY)));
		formPrefill.setIdentities(Map.of(1, new PrefilledEntry<IdentityParam>(idParam1, PrefilledEntryMode.HIDDEN)));
		formPrefill.setMessageParams(Map.of("mpk1", "mpv1"));

		ExpectedIdentity expectedIdentity = new ExpectedIdentity("identity", IdentityExpectation.MANDATORY);

		RegistrationInvitationParam registrationInvitationParam = RegistrationInvitationParam.builder()
				.withContactAddress("contactAddress")
				.withExpiration(Instant.ofEpochMilli(1))
				.withForm(formPrefill)
				.withInviter(1L)
				.withExpectedIdentity(expectedIdentity)
				.build();

		InvitationWithCode invitationWithCode = new InvitationWithCode(registrationInvitationParam, "code",
				Instant.ofEpochMilli(1), 1);
		invitationWithCode.setCreationTime(Instant.ofEpochMilli(1));
		return invitationWithCode;

	}

	@Override
	protected DBInvitationWithCode getFullDBObject()
	{
		ObjectNode meta = Constants.MAPPER.createObjectNode();
		meta.put("1", "v");

		return DBInvitationWithCode.builder()
				.withNumberOfSends(1)
				.withCreationTime(Instant.ofEpochMilli(1))
				.withNumberOfSends(1)
				.withLastSentTime(Instant.ofEpochMilli(1))
				.withRegistrationCode("code")
				.withInvitation(DBRegistrationInvitationParam.builder()
						.withExpectedIdentity(DBExpectedIdentity.builder()
								.withExpectation("MANDATORY")
								.withIdentity("identity")
								.build())
						.withExpiration(1L)
						.withInviter(1L)
						.withType("REGISTRATION")
						.withContactAddress("contactAddress")
						.withFormPrefill(DBFormPrefill.builder()
								.withFormId("formId")
								.withAllowedGroups(Map.of(1, DBGroupSelection.builder()
										.withExternalIdp("externalIdp")
										.withTranslationProfile("Profile")
										.withSelectedGroups(List.of("/g1", "/g2"))
										.build()))
								.withAttributes(Map.of(1,
										new DBPrefilledEntry.Builder<DBAttribute>()
												.withEntry(DBAttribute.builder()
														.withName("attr")
														.withValueSyntax("syntax")
														.withGroupPath("/A")
														.withValues(List.of("v1", "v2"))
														.withRemoteIdp("remoteIdp")
														.withTranslationProfile("translationProfile")
														.build())
												.withMode("READ_ONLY")
												.build()))
								.withGroupSelections(Map.of(1,
										new DBPrefilledEntry.Builder<DBGroupSelection>()
												.withEntry(DBGroupSelection.builder()
														.withExternalIdp("externalIdp")
														.withTranslationProfile("Profile")
														.withSelectedGroups(List.of("/g1", "/g2"))
														.build())
												.withMode("READ_ONLY")
												.build()))
								.withMessageParams(Map.of("mpk1", "mpv1"))
								.withIdentities(Map.of(1,
										new DBPrefilledEntry.Builder<DBIdentityParam>()
												.withEntry(DBIdentityParam.builder()
														.withValue("test@wp.pl")
														.withTypeId("email")
														.withRealm("realm")
														.withRemoteIdp("remoteIdp")
														.withTarget("target")
														.withMetadata(meta)
														.withTranslationProfile("Profile")
														.withConfirmationInfo(DBConfirmationInfo.builder()
																.withSentRequestAmount(1)
																.withConfirmed(true)
																.withConfirmationDate(1L)
																.build())
														.build())
												.withMode("HIDDEN")
												.build()))
								.build())
						.build())

				.build();
	}

	@Override
	protected Pair<Function<InvitationWithCode, DBInvitationWithCode>, Function<DBInvitationWithCode, InvitationWithCode>> getMapper()
	{
		return Pair.of(InvitationWithCodeMapper::map, InvitationWithCodeMapper::map);
	}


}
