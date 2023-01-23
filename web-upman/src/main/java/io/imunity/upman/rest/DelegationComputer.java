/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.RegistrationForm;

import java.util.List;

class DelegationComputer
{
	private final String fullGroupName;
	private final String logoUrl;
	private final List<String> readOnlyAttributes;
	private final GroupDelegationConfigGenerator groupDelegationConfigGenerator;
	private final RegistrationsManagement registrationsManagement;
	private final EnquiryManagement enquiryManagement;

	private DelegationComputer(String fullGroupName, String logoUrl,
	                           List<String> readOnlyAttributes, GroupDelegationConfigGenerator groupDelegationConfigGenerator,
	                           RegistrationsManagement registrationsManagement, EnquiryManagement enquiryManagement)
	{
		this.fullGroupName = fullGroupName;
		this.logoUrl = logoUrl;
		this.readOnlyAttributes = readOnlyAttributes;
		this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
		this.registrationsManagement = registrationsManagement;
		this.enquiryManagement = enquiryManagement;
	}

	public String computeMembershipUpdateEnquiryName(RestMembershipEnquiry membershipUpdateEnquiry) throws EngineException
	{
		String updateEnquiryName = membershipUpdateEnquiry.name;
		if (membershipUpdateEnquiry.autogenerate)
		{
			EnquiryForm updateEnquiryForm = groupDelegationConfigGenerator
				.generateProjectUpdateEnquiryForm(
					fullGroupName,
					logoUrl);
			enquiryManagement.addEnquiry(updateEnquiryForm);
			updateEnquiryName = updateEnquiryForm.getName();
		}
		return updateEnquiryName;
	}

	public String computeSignUpEnquiryName(RestSignUpEnquiry signUpEnquiry) throws EngineException
	{
		String joinEnquiryName = signUpEnquiry.name;
		if (signUpEnquiry.autogenerate)
		{
			EnquiryForm joinEnquiryForm = groupDelegationConfigGenerator
				.generateProjectJoinEnquiryForm(
					fullGroupName,
					logoUrl);
			enquiryManagement.addEnquiry(joinEnquiryForm);
			joinEnquiryName = joinEnquiryForm.getName();
		}
		return joinEnquiryName;
	}

	public String computeRegistrationFormName(RestRegistrationForm registrationForm) throws EngineException
	{
		String registrationFormName = registrationForm.name;
		if (registrationForm.autogenerate)
		{
			RegistrationForm regForm = groupDelegationConfigGenerator
				.generateProjectRegistrationForm(
					fullGroupName, logoUrl, readOnlyAttributes);
			registrationsManagement.addForm(regForm);
			registrationFormName = regForm.getName();
		}
		return registrationFormName;
	}

	public static DelegationSetterBuilder builder()
	{
		return new DelegationSetterBuilder();
	}

	public static final class DelegationSetterBuilder
	{
		private String fullGroupName;
		private String logoUrl;
		private List<String> readOnlyAttributes;
		private GroupDelegationConfigGenerator groupDelegationConfigGenerator;
		private RegistrationsManagement registrationsManagement;
		private EnquiryManagement enquiryManagement;

		private DelegationSetterBuilder()
		{
		}

		public DelegationSetterBuilder withFullGroupName(String fullGroupName)
		{
			this.fullGroupName = fullGroupName;
			return this;
		}

		public DelegationSetterBuilder withLogoUrl(String logoUrl)
		{
			this.logoUrl = logoUrl;
			return this;
		}

		public DelegationSetterBuilder withReadOnlyAttributes(List<String> readOnlyAttributes)
		{
			this.readOnlyAttributes = readOnlyAttributes;
			return this;
		}

		public DelegationSetterBuilder withGroupDelegationConfigGenerator(GroupDelegationConfigGenerator groupDelegationConfigGenerator)
		{
			this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
			return this;
		}

		public DelegationSetterBuilder withRegistrationsManagement(RegistrationsManagement registrationsManagement)
		{
			this.registrationsManagement = registrationsManagement;
			return this;
		}

		public DelegationSetterBuilder withEnquiryManagement(EnquiryManagement enquiryManagement)
		{
			this.enquiryManagement = enquiryManagement;
			return this;
		}

		public DelegationComputer build()
		{
			return new DelegationComputer(fullGroupName, logoUrl, readOnlyAttributes, groupDelegationConfigGenerator, registrationsManagement, enquiryManagement);
		}
	}
}
