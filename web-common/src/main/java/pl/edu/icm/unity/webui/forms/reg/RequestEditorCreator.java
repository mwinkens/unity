/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.common.policyAgreement.PolicyAgreementRepresentationBuilder;
import pl.edu.icm.unity.webui.forms.FormsInvitationHelper;
import pl.edu.icm.unity.webui.forms.RegCodeException;
import pl.edu.icm.unity.webui.forms.URLQueryPrefillCreator;
import pl.edu.icm.unity.webui.forms.RegCodeException.ErrorCause;

/**
 * Creates instances of {@link RegistrationRequestEditor}. May ask for a registration/invitation code if needed first
 * and handles loading of related invitation if needed.
 * 
 * @author Krzysztof Benedyczak
 */
@PrototypeComponent
public class RequestEditorCreator
{
	private MessageSource msg;
	private ImageAccessService imageAccessService;
	private RegistrationForm form;
	private RemotelyAuthenticatedPrincipal remotelyAuthenticated;
	private IdentityEditorRegistry identityEditorRegistry;
	private CredentialEditorRegistry credentialEditorRegistry;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private AttributeTypeManagement aTypeMan;
	private GroupsManagement groupsMan;
	private CredentialManagement credMan;
	private AuthenticatorSupportService authnSupport;
	private String registrationCode;
	private FormsInvitationHelper invitationHelper;
	private URLQueryPrefillCreator urlQueryPrefillCreator;
	private PolicyAgreementRepresentationBuilder policyAgreementsRepresentationBuilder;
	private boolean enableRemoteSignup;

	@Autowired
	public RequestEditorCreator(MessageSource msg, ImageAccessService imageAccessService,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			@Qualifier("insecure") AttributeTypeManagement aTypeMan,
			@Qualifier("insecure") GroupsManagement groupsMan, 
			@Qualifier("insecure") CredentialManagement credMan,
			@Qualifier("insecure") InvitationManagement invitationMan,
			AuthenticatorSupportService authnSupport,
			URLQueryPrefillCreator urlQueryPrefillCreator,
			PolicyAgreementRepresentationBuilder policyAgreementsRepresentationBuilder)
	{
		this.msg = msg;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.aTypeMan = aTypeMan;
		this.groupsMan = groupsMan;
		this.credMan = credMan;
		this.urlQueryPrefillCreator = urlQueryPrefillCreator;
		this.invitationHelper = new FormsInvitationHelper(invitationMan);
		this.authnSupport = authnSupport;
		this.imageAccessService = imageAccessService;
		this.policyAgreementsRepresentationBuilder = policyAgreementsRepresentationBuilder;
	}
	

	public RequestEditorCreator init(RegistrationForm form, boolean enableRemoteSignup,
			RemotelyAuthenticatedPrincipal context, String presetRegistrationCode)
	{
		this.form = form;
		this.enableRemoteSignup = enableRemoteSignup;
		this.remotelyAuthenticated = context;
		this.registrationCode = presetRegistrationCode;
		return this;
	}
	
	public RequestEditorCreator init(RegistrationForm form, RemotelyAuthenticatedPrincipal context)
	{
		return init(form, false, context, null);
	}

	public void createFirstStage(RequestEditorCreatedCallback callback, Runnable onLocalSignupHandler)
	{
		if (registrationCode == null)
			registrationCode = RegistrationFormDialogProvider.getCodeFromURL();
		
		if (registrationCode == null && form.isByInvitationOnly())
		{
			askForCode(callback, () -> doCreateFirstStage(callback, onLocalSignupHandler));
		} else
		{
			doCreateFirstStage(callback, onLocalSignupHandler);
		}
	}
	
	public void createSecondStage(RequestEditorCreatedCallback callback, boolean withCredentials)
	{
		if (registrationCode == null && form.isByInvitationOnly())
		{
			askForCode(callback, () -> doCreateSecondStage(callback, withCredentials));
		} else
		{
			doCreateSecondStage(callback, withCredentials);
		}
	}

	private void doCreateFirstStage(RequestEditorCreatedCallback callback, Runnable onLocalSignupHandler)
	{
		InvitationParam invitation;
		try
		{
			invitation = getInvitationByCode(registrationCode);
		} catch (RegCodeException e1)
		{
			callback.onCreationError(e1, e1.cause);
			return;
		}
		
		try
		{
			RegistrationRequestEditor editor = doCreateEditor(registrationCode, (RegistrationInvitationParam) invitation);
			editor.showFirstStage(onLocalSignupHandler);
			callback.onCreated(editor);
		} catch (AuthenticationException e)
		{
			callback.onCreationError(e, ErrorCause.MISCONFIGURED);
		}
	}

	
	private void doCreateSecondStage(RequestEditorCreatedCallback callback, boolean withCredentials)
	{
		InvitationParam invitation;
		try
		{
			invitation = getInvitationByCode(registrationCode);
		} catch (RegCodeException e1)
		{
			callback.onCreationError(e1, e1.cause);
			return;
		}
		try
		{
			RegistrationRequestEditor editor = doCreateEditor(registrationCode, (RegistrationInvitationParam) invitation);
			editor.showSecondStage(withCredentials);
			callback.onCreated(editor);
		} catch (AuthenticationException e)
		{
			callback.onCreationError(e, ErrorCause.MISCONFIGURED);
		}
	}
	
	private void askForCode(RequestEditorCreatedCallback callback, Runnable uiCreator)
	{
		GetRegistrationCodeDialog askForCodeDialog = new GetRegistrationCodeDialog(msg, 
				new GetRegistrationCodeDialog.Callback()
		{
			@Override
			public void onCodeGiven(String code)
			{
				registrationCode = code;
				uiCreator.run();
			}
			
			@Override
			public void onCancel()
			{
				callback.onCancel();
			}
		}, msg.getMessage("GetRegistrationCodeDialog.title"),
		msg.getMessage("GetRegistrationCodeDialog.information"),
		msg.getMessage("GetRegistrationCodeDialog.code"));
		askForCodeDialog.show();
	}

	private RegistrationRequestEditor doCreateEditor(String registrationCode, 
			RegistrationInvitationParam invitation) 
			throws AuthenticationException
	{
		return new RegistrationRequestEditor(msg, form, 
				remotelyAuthenticated, identityEditorRegistry, 
				credentialEditorRegistry, attributeHandlerRegistry, 
				aTypeMan, credMan, groupsMan, imageAccessService,
				registrationCode, invitation, authnSupport,  
				urlQueryPrefillCreator, policyAgreementsRepresentationBuilder, enableRemoteSignup);
	}
	
	private InvitationParam getInvitationByCode(String registrationCode) throws RegCodeException
	{
		if (form.isByInvitationOnly() && registrationCode == null)
			throw new RegCodeException(ErrorCause.MISSING_CODE);

		InvitationParam invitation = invitationHelper.getInvitationByCode(registrationCode, InvitationType.REGISTRATION);
		
		if (invitation != null && !invitation.getFormId().equals(form.getName()))
			throw new RegCodeException(ErrorCause.INVITATION_OF_OTHER_FORM);
		if (form.isByInvitationOnly() &&  invitation == null)
			throw new RegCodeException(ErrorCause.UNRESOLVED_INVITATION);
		if (form.isByInvitationOnly() &&  invitation.isExpired())
			throw new RegCodeException(ErrorCause.EXPIRED_INVITATION);
		return invitation;
	}
	
	public interface RequestEditorCreatedCallback
	{
		void onCreated(RegistrationRequestEditor editor);
		void onCreationError(Exception e, ErrorCause cause);
		void onCancel();
	}
}
