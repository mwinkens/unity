/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.reg;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.engine.forms.BaseRequestPreprocessor;
import pl.edu.icm.unity.engine.forms.InvitationPrefillInfo;
import pl.edu.icm.unity.engine.forms.PolicyAgreementsValidator;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;

/**
 * Helper component with methods to validate registration requests. There are methods to validate both the request 
 * being submitted (i.e. whether it is valid wrt its form) and validating the translated request before it is 
 * accepted.
 * <p>
 * What is more this component implements invitations handling, i.e. the overall validation and 
 * updating the request with mandatory invitation information (what must be done prior to base validation).
 * Also attributes configured to use contextual groups are updated with final group here.
 * 
 * @author K. Benedyczak
 */
@Component
public class RegistrationRequestPreprocessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_FORMS,
			RegistrationRequestPreprocessor.class);
	
	private final PolicyAgreementsValidator agreementValidator;
	private final BaseRequestPreprocessor basePreprocessor;
	
	@Autowired
	public RegistrationRequestPreprocessor(PolicyAgreementsValidator agreementValidator,
			BaseRequestPreprocessor basePreprocessor)
	{
		this.agreementValidator = agreementValidator;
		this.basePreprocessor = basePreprocessor;
	}

	public void validateSubmittedRequest(RegistrationForm form, RegistrationRequest request,
			boolean doCredentialCheckAndUpdate) throws EngineException
	{
		validateSubmittedRequest(form, request, doCredentialCheckAndUpdate, false);
	}
	
	public void validateSubmittedRequestExceptCredentials(RegistrationForm form, RegistrationRequest request,
			boolean doCredentialCheckAndUpdate) throws EngineException
	{
		validateSubmittedRequest(form, request, doCredentialCheckAndUpdate, true);
	}
	
	private void validateSubmittedRequest(RegistrationForm form, RegistrationRequest request,
			boolean doCredentialCheckAndUpdate, boolean skipCredentialsValidation) throws EngineException
	{
		InvitationPrefillInfo invitationInfo = processInvitationAndValidateCode(form, request);
		
		basePreprocessor.validateSubmittedRequest(form, request, doCredentialCheckAndUpdate, skipCredentialsValidation);
		agreementValidator.validate(form, request);
		
		applyContextGroupsToAttributes(form, request);

		if (invitationInfo.isByInvitation())
		{
			String code = request.getRegistrationCode();
			log.info("Received registration request for invitation {}, removing it", code);
			basePreprocessor.removeInvitation(code);
		}
	}

	public void validateTranslatedRequest(RegistrationForm form, RegistrationRequest originalRequest, 
			TranslatedRegistrationRequest request) throws EngineException
	{
		basePreprocessor.validateFinalAttributes(request.getAttributes());
		basePreprocessor.validateFinalCredentials(originalRequest.getCredentials());
		basePreprocessor.validateFinalIdentities(request.getIdentities());
		basePreprocessor.validateFinalGroups(request.getGroups());
	}

	private void applyContextGroupsToAttributes(RegistrationForm form, RegistrationRequest request) 
			throws IllegalFormContentsException
	{
		Map<String, Integer> wildcardToGroupParamIndex = new HashMap<>();
		int j=0;
		for (GroupRegistrationParam groupParam: form.getGroupParams())
			wildcardToGroupParamIndex.put(groupParam.getGroupPath(), j++);
		
		for (int i = 0; i < request.getAttributes().size(); i++)
		{
			Attribute attr = request.getAttributes().get(i);
			if (attr == null)
				continue;
			
			applyContextGroupToAttributeIfNeeded(attr, form, i, idx -> request.getGroupSelections().get(idx), 
					wildcardToGroupParamIndex);
		}
	}
	
	public static void applyContextGroupToAttributeIfNeeded(Attribute attr, RegistrationForm form, int i, 
			Function<Integer, GroupSelection> groupResolver, Map<String, Integer> wildcardToGroupParamIndex) 
					throws IllegalFormContentsException
	{
		AttributeRegistrationParam regParam = form.getAttributeParams().get(i);
		if (regParam.isUsingDynamicGroup())
		{
			String wildcard = regParam.getDynamicGroup();
			Integer index = wildcardToGroupParamIndex.get(wildcard);
			if (index == null)
				throw new IllegalStateException("Form is inconsistent: "
						+ "no group param for dynamic attribute using group wildcard " 
						+ wildcard);
			GroupSelection resolvedGroup = groupResolver.apply(index);
			if (resolvedGroup == null)
				throw new IllegalFormContentsException("Group must be selected for parameter " 
						+ form.getGroupParams().get(index).getLabel());
			if (resolvedGroup.getSelectedGroups().size() != 1)
				throw new IllegalFormContentsException("Single group must be selected for parameter " 
						+ form.getGroupParams().get(index).getLabel());
			attr.setGroupPath(resolvedGroup.getSelectedGroups().get(0));
		}
	}
	
	
	/**
	 * Code is validated, wrt to invitation or form fixed code. What is more the request attributes
	 * groups and identities are set to those from invitation when necessary and errors are reported
	 * if request tries to overwrite mandatory elements from invitation.
	 */
	private InvitationPrefillInfo processInvitationAndValidateCode(RegistrationForm form, RegistrationRequest request) 
			throws IllegalFormContentsException
	{
		String codeFromRequest = request.getRegistrationCode();

		if (codeFromRequest == null && form.isByInvitationOnly())
			throw new IllegalFormContentsException("This registration form is available "
					+ "only by invitation with correct code");
		
		if (codeFromRequest == null || form.getRegistrationCode() != null)
		{
			validateRequestCode(form, request);
			return new InvitationPrefillInfo();
		}
				
		InvitationParam invitation = basePreprocessor.getInvitation(codeFromRequest).getInvitation();
		InvitationPrefillInfo invitationInfo = new InvitationPrefillInfo(true);
		
		if (!invitation.getFormId().equals(form.getName()))
			throw new IllegalFormContentsException("The invitation is for different registration form");
		
		if (invitation.isExpired())
			throw new IllegalFormContentsException("The invitation has already expired");
		
		log.debug("Will apply invitation parameter to the request:\n{}", invitation.toString());
		log.debug("Request before applying the invitation:\n{}", request.toString());
		basePreprocessor.processInvitationElements(form.getIdentityParams(), request.getIdentities(), 
				invitation.getIdentities(), "identity");
		basePreprocessor.processInvitationElements(form.getAttributeParams(), request.getAttributes(), 
				invitation.getAttributes(), "attribute");
		basePreprocessor.processInvitationElements(form.getGroupParams(), request.getGroupSelections(), 
				basePreprocessor.filterValueReadOnlyAndHiddenGroupFromInvitation(invitation.getGroupSelections(), form.getGroupParams()), 
				"group");
		return invitationInfo;
	}

	private void validateRequestCode(RegistrationForm form, RegistrationRequest request)
			throws IllegalFormContentsException
	{
		String formCode = form.getRegistrationCode();
		String code = request.getRegistrationCode();
		if (formCode != null && code == null)
			throw new IllegalFormContentsException("This registration "
					+ "form require a registration code.");
		if (formCode != null && code != null && !formCode.equals(code))
			throw new IllegalFormContentsException("The registration code is invalid.");
	}	
}
