/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.registration.ExternalSignupGridSpec;
import pl.edu.icm.unity.types.registration.ExternalSignupGridSpec.AuthnGridSettings;
import pl.edu.icm.unity.types.registration.FormLayoutUtils;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;
import pl.edu.icm.unity.types.registration.layout.BasicFormElement;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.types.registration.layout.FormLocalSignupButtonElement;
import pl.edu.icm.unity.types.registration.layout.FormParameterElement;
import pl.edu.icm.unity.webui.authn.PreferredAuthenticationHelper;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationCapable;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationFilter;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.Context;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.column.AuthNOption;
import pl.edu.icm.unity.webui.authn.column.AuthNPanelFactory;
import pl.edu.icm.unity.webui.authn.column.AuthnsGridWidget;
import pl.edu.icm.unity.webui.authn.column.FirstFactorAuthNPanel;
import pl.edu.icm.unity.webui.authn.column.SearchComponent;
import pl.edu.icm.unity.webui.common.CaptchaComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.common.policyAgreement.PolicyAgreementRepresentationBuilder;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.forms.BaseRequestEditor;
import pl.edu.icm.unity.webui.forms.PrefilledSet;
import pl.edu.icm.unity.webui.forms.RegistrationLayoutsContainer;
import pl.edu.icm.unity.webui.forms.URLQueryPrefillCreator;

/**
 * Generates a UI based on a given registration form. User can fill the form and a request is returned.
 * The class verifies if the data obtained from an upstream IdP is complete wrt requirements of the form.
 * <p>
 * Objects of this class should be typically created using {@link RequestEditorCreator}, so that the
 * registration code is collected appropriately.
 * @author K. Benedyczak
 */
public class RegistrationRequestEditor extends BaseRequestEditor<RegistrationRequest>
{
	enum Stage {FIRST, SECOND}
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, RegistrationRequestEditor.class);
	private RegistrationForm form;
	
	private TextField registrationCode;
	private CaptchaComponent captcha;
	private String regCodeProvided;
	private RegistrationInvitationParam invitation;
	private AuthenticatorSupportService authnSupport;
	private SignUpAuthNController signUpAuthNController;
	private Map<AuthenticationOptionKey, AuthNOption> externalSignupOptions;
	private Runnable onLocalSignupHandler;
	private FormLayout effectiveLayout;
	private Stage stage;
	private RegistrationLayoutsContainer layoutContainer;
	private URLQueryPrefillCreator urlQueryPrefillCreator;

	/**
	 * Note - the two managers must be insecure, if the form is used in not-authenticated context, 
	 * what is possible for registration form.
	 */
	public RegistrationRequestEditor(MessageSource msg, RegistrationForm form,
			RemotelyAuthenticatedContext remotelyAuthenticated,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributeTypeManagement aTypeMan, CredentialManagement credMan,
			GroupsManagement groupsMan, ImageAccessService imageAccessService,
			String registrationCode, RegistrationInvitationParam invitation2, 
			AuthenticatorSupportService authnSupport, 
			SignUpAuthNController signUpAuthNController,
			URLQueryPrefillCreator urlQueryPrefillCreator, 
			PolicyAgreementRepresentationBuilder policyAgreementsRepresentationBuilder)
	{
		super(msg, form, remotelyAuthenticated, identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, aTypeMan, credMan, groupsMan, imageAccessService,
				policyAgreementsRepresentationBuilder);
		this.form = form;
		this.regCodeProvided = registrationCode;
		this.invitation = invitation2;
		this.signUpAuthNController = signUpAuthNController;
		this.authnSupport = authnSupport;
		this.urlQueryPrefillCreator = urlQueryPrefillCreator;
	}
	
	public void showFirstStage(Runnable onLocalSignupHandler) throws AuthenticationException
	{
		this.effectiveLayout = form.getEffectivePrimaryFormLayout(msg);
		this.onLocalSignupHandler = onLocalSignupHandler;
		this.stage = Stage.FIRST;
		if (form.isLocalSignupEnabled()) //when we have only remote signup enabled, validation must be deferred to 2nd stage
			validateMandatoryRemoteInput(); 
		initUI();
	}
	
	public void showSecondStage(boolean withCredentials) throws AuthenticationException
	{
		this.effectiveLayout = withCredentials ? form.getEffectiveSecondaryFormLayout(msg) 
				: form.getEffectiveSecondaryFormLayoutWithoutCredentials(msg);
		this.stage = Stage.SECOND;
		validateMandatoryRemoteInput();
		initUI();
	}
	
	@Override
	public RegistrationRequest getRequest(boolean withCredentials) throws FormValidationException
	{
		//defensive check: if we have local signup button then submission makes no sense - 
		//we need to go to 2nd stage first. 
		if (FormLayoutUtils.hasLocalSignupButton(effectiveLayout))
			throw new FormValidationException(msg.getMessage("RegistrationRequest.continueRegistration"));
		
		RegistrationRequest ret = new RegistrationRequest();
		FormErrorStatus status = new FormErrorStatus();

		super.fillRequest(ret, status, withCredentials);
		
		setRequestCode(ret, status);
		if (captcha != null)
		{
			try
			{
				captcha.verify();
			} catch (WrongArgumentException e)
			{
				status.hasFormException = true;
			}
		}
		
		if (status.hasFormException)
			throw new FormValidationException(status.errorMsg);
		
		return ret;
	}
	
	/**
	 * @return true if the editor can be submitted without the subsequent stage
	 */
	public boolean isSubmissionPossible()
	{
		return (stage == Stage.FIRST && form.isLocalSignupEnabled() && !FormLayoutUtils.hasLocalSignupButton(effectiveLayout)) 
				|| stage == Stage.SECOND;
	}

	Stage getStage()
	{
		return stage;
	}
	
	private void setRequestCode(RegistrationRequest ret, FormErrorStatus status)
	{
		if (form.getRegistrationCode() != null && regCodeProvided == null)
		{
			ret.setRegistrationCode(registrationCode.getValue());
			if (registrationCode.getValue().isEmpty())
			{
				registrationCode.setComponentError(new UserError(msg.getMessage("fieldRequired")));
				status.hasFormException = true;
			} else
				registrationCode.setComponentError(null);
		}
		
		if (invitation != null)
			ret.setRegistrationCode(regCodeProvided);
	}
	

	void focusFirst()
	{
		focusFirst(layoutContainer.registrationFormLayout);
	}
	
	private void initUI()
	{
		layoutContainer = createLayouts(
				invitation != null
						? invitation.getMessageParamsWithCustomVarObject(
								MessageTemplateDefinition.CUSTOM_VAR_PREFIX)
						: Collections.emptyMap());

		resolveRemoteSignupOptions();
		PrefilledSet prefilled = new PrefilledSet();
		if (invitation != null)
		{
			prefilled = new PrefilledSet(invitation.getIdentities(),
					invitation.getGroupSelections(),
					invitation.getAttributes(),
					invitation.getAllowedGroups());
		}
		prefilled = prefilled.mergeWith(urlQueryPrefillCreator.create(form));
		createControls(layoutContainer, effectiveLayout, prefilled);
	}
	
	boolean performAutomaticRemoteSignupIfNeeded()
	{
		if (isAutomatedAuthenticationDesired() && externalSignupOptions.size() > 0)
		{
			VaadinServletRequest httpRequest = (VaadinServletRequest) VaadinRequest.getCurrent();
			String requestedAuthnOption = httpRequest.getParameter(PreferredAuthenticationHelper.IDP_SELECT_PARAM);
			if (externalSignupOptions.size() > 1 && requestedAuthnOption == null)
			{
				log.warn("There are more multiple remote signup options are installed, "
						+ "and automated signup was requested without specifying (with " 
						+  "{}) which one should be used. Automatic signup is skipped.", 
						PreferredAuthenticationHelper.IDP_SELECT_PARAM);
				return false;
			}
			AuthNOption authnOption = requestedAuthnOption != null ? 
					externalSignupOptions.get(AuthenticationOptionKey.valueOf(requestedAuthnOption)) : 
					externalSignupOptions.values().iterator().next();
			if (authnOption == null)
			{
				log.warn("Remote signup option {} specified for auto signup is invalid. "
						+ "Automatic signup is skipped.", requestedAuthnOption);
				return false;
			}
			if (authnOption.authenticator instanceof ProxyAuthenticationCapable)
			{
				ProxyAuthenticationCapable proxyAuthn = (ProxyAuthenticationCapable) authnOption.authenticator;
				proxyAuthn.triggerAutomatedUIAuthentication(authnOption.authenticatorUI);
				return true;
			} else
			{
				log.warn("Automatic signup was requested but the selected remote authenticator "
						+ "is not capable of automatic triggering");
				return false;
			}
		}
		return false;
	}
	
	private boolean isAutomatedAuthenticationDesired()
	{
		VaadinServletRequest httpRequest = (VaadinServletRequest) VaadinRequest.getCurrent();
		String autoLogin = httpRequest.getParameter(ProxyAuthenticationFilter.TRIGGERING_PARAM);
		if (autoLogin != null && Boolean.parseBoolean(autoLogin))
			return true;
		return false;
	}

	@Override
	protected RegistrationLayoutsContainer createLayouts(Map<String, Object> params)
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(false);
		main.setWidth(100, Unit.PERCENTAGE);
		setCompositionRoot(main);
		
		String logoUri = form.getLayoutSettings().getLogoURL();
		Optional<Resource> logoRes = imageAccessService.getConfiguredImageResourceFromNullableUri(logoUri);
		if (logoRes.isPresent())
		{
			Image image = new Image(null, logoRes.get());
			image.addStyleName("u-signup-logo");
			main.addComponent(image);
			main.setComponentAlignment(image, Alignment.TOP_CENTER);	
		}
		
		I18nString title = stage == Stage.FIRST ? form.getDisplayedName() : form.getTitle2ndStage();
		Label formName = new Label(processFreeemarkerTemplate(params, title.getValue(msg)));
		formName.addStyleName(Styles.vLabelH1.toString());
		formName.addStyleName("u-reg-title");
		main.addComponent(formName);
		main.setComponentAlignment(formName, Alignment.MIDDLE_CENTER);
		
		if (stage == Stage.FIRST)
		{
			String info = form.getFormInformation() == null ? null : processFreeemarkerTemplate(params, form.getFormInformation().getValue(msg));
			if (info != null)
			{
				HtmlConfigurableLabel formInformation = new HtmlConfigurableLabel(info);
				formInformation.addStyleName("u-reg-info");
				main.addComponent(formInformation);
				main.setComponentAlignment(formInformation, Alignment.MIDDLE_CENTER);
			}
		}
		
		RegistrationLayoutsContainer container = new RegistrationLayoutsContainer(formWidth(), formWidthUnit());
		container.addFormLayoutToRootLayout(main);
		return container;
	}
	
	private void resolveRemoteSignupOptions()
	{
		externalSignupOptions = new HashMap<>();
		if (!form.getExternalSignupSpec().isEnabled())
			return;
		
		Set<String> authnOptions = form.getExternalSignupSpec().getSpecs().stream()
			.map(AuthenticationOptionsSelector::getAuthenticatorKey)
			.collect(Collectors.toSet());
		List<AuthenticationFlow> flows = authnSupport.resolveAuthenticationFlows(Lists.newArrayList(authnOptions),
				VaadinAuthentication.NAME);
		Set<AuthenticationOptionsSelector> formSignupSpec = form.getExternalSignupSpec().getSpecs().stream()
				.collect(Collectors.toSet());
		for (AuthenticationFlow flow : flows)
		{
			for (AuthenticatorInstance authenticator : flow.getFirstFactorAuthenticators())
			{
				VaadinAuthentication vaadinAuthenticator = (VaadinAuthentication) authenticator.getRetrieval();
				String authenticatorKey = vaadinAuthenticator.getAuthenticatorId();
				Collection<VaadinAuthenticationUI> optionUIInstances = vaadinAuthenticator.createUIInstance(Context.REGISTRATION);
				for (VaadinAuthenticationUI vaadinAuthenticationUI : optionUIInstances)
				{
					String optionKey = vaadinAuthenticationUI.getId();
					AuthenticationOptionKey authnOption = new AuthenticationOptionKey(authenticatorKey, optionKey);
					if (formSignupSpec.stream().anyMatch(selector -> selector.matchesAuthnOption(authnOption)))
					{
						AuthNOption signupAuthNOption = new AuthNOption(flow, vaadinAuthenticator,  vaadinAuthenticationUI);
						setupExpectedIdentity(vaadinAuthenticationUI);
						externalSignupOptions.put(authnOption, signupAuthNOption);
					}
				}
			}
		}
	}

	private void setupExpectedIdentity(VaadinAuthenticationUI vaadinAuthenticationUI)
	{
		if (invitation == null)
			return;
		if (invitation.getExpectedIdentity() != null)
			vaadinAuthenticationUI.setExpectedIdentity(invitation.getExpectedIdentity());
	}
	
	@Override
	protected boolean createControlFor(RegistrationLayoutsContainer layoutContainer, FormElement element, 
			FormElement previousAdded, FormElement next, PrefilledSet prefilled)
	{
		switch (element.getType())
		{
		case CAPTCHA:
			return createCaptchaControl(layoutContainer.registrationFormLayout, (BasicFormElement) element);
		case REG_CODE:
			return createRegistrationCodeControl(layoutContainer.registrationFormLayout, (BasicFormElement) element);
		case REMOTE_SIGNUP:
			return createRemoteSignupButton(layoutContainer.registrationFormLayout, (FormParameterElement) element);
		case REMOTE_SIGNUP_GRID:
			return createRemoteSignupGrid(layoutContainer.registrationFormLayout, (FormParameterElement) element);
		case LOCAL_SIGNUP:
			return createLocalSignupButton(layoutContainer.registrationFormLayout, (FormLocalSignupButtonElement) element);
		default:
			return super.createControlFor(layoutContainer, element, previousAdded, next, prefilled);
		}
	}

	private boolean createRemoteSignupGrid(VerticalLayout registrationFormLayout, FormParameterElement element)
	{			
		ExternalSignupGridSpec externalSignupGridSpec = form.getExternalSignupGridSpec();
		AuthnGridSettings gridSettings = externalSignupGridSpec.getGridSettings();
		if (gridSettings == null)
		{
			gridSettings = new AuthnGridSettings();
		}
		
		List<AuthNOption> options = new ArrayList<>();
		for (AuthenticationOptionsSelector spec : externalSignupGridSpec.getSpecs())
		{
			List<AuthNOption> signupOptions = getSignupOptions(spec);
			if (signupOptions.isEmpty())
			{
				log.debug("Ignoring not available remote sign up options: {}", spec.toStringEncodedSelector());
			}
			
			options.addAll(signupOptions);
		}
		
		if (options.isEmpty())
		{
			log.debug("All signup options are not available, skipping add remote sigup grid");
			return false;
		}
		
		AuthnsGridWidget grid = new AuthnsGridWidget(options, msg, new RegGridAuthnPanelFactory(), gridSettings.height);
		grid.setWidth(formWidth(), formWidthUnit());
		SearchComponent search = new SearchComponent(msg, grid::filter);
		if (gridSettings.searchable)
		{
			registrationFormLayout.addComponent(search);
			registrationFormLayout.setComponentAlignment(search, Alignment.MIDDLE_RIGHT);
		}
		
		registrationFormLayout.addComponent(grid);
		registrationFormLayout.setComponentAlignment(grid, Alignment.MIDDLE_CENTER);
		if(signUpAuthNController == null)
		{
			grid.setEnabled(false); //for some UIs (admin) we can't really trigger external authN
		}
	
		return true;		
	}

	private boolean createRemoteSignupButton(AbstractOrderedLayout layout, FormParameterElement element)
	{
		int index = element.getIndex();
		AuthenticationOptionsSelector spec = form.getExternalSignupSpec().getSpecs().get(index);

		List<AuthNOption> options = getSignupOptions(spec);
		if (options.isEmpty())
		{
			log.debug("Ignoring not available remote sign up option {}", spec.toStringEncodedSelector());
			return false;
		}

		for (AuthNOption option : options)
		{
			Component signupOptionComponent = option.authenticatorUI.getComponent();
			signupOptionComponent.setWidth(formWidth(), formWidthUnit());
			layout.addComponent(signupOptionComponent);
			layout.setComponentAlignment(signupOptionComponent, Alignment.MIDDLE_CENTER);

			if (signUpAuthNController == null)
			{
				signupOptionComponent.setEnabled(false); //for some UIs (admin) we can't really trigger external authN
			} else
			{
				option.authenticatorUI
						.setAuthenticationCallback(signUpAuthNController.buildCallback(option));
			}
		}

		return true;
	}

	private List<AuthNOption> getSignupOptions(AuthenticationOptionsSelector spec)
	{
		return externalSignupOptions.entrySet().stream()
					.filter(e -> spec.matchesAuthnOption(e.getKey()))
					.map(e -> e.getValue()).collect(Collectors.toList());
	}
	
	private boolean createLocalSignupButton(AbstractOrderedLayout layout, FormLocalSignupButtonElement element)
	{
		Button localSignup = new Button(msg.getMessage("RegistrationRequest.localSignup"));
		localSignup.addStyleName("u-localSignUpButton");
		localSignup.addClickListener(event -> onLocalSignupHandler.run());
		localSignup.setWidth(formWidth(), formWidthUnit());
		layout.addComponent(localSignup);
		layout.setComponentAlignment(localSignup, Alignment.MIDDLE_CENTER);
		return true;
	}
	
	private boolean createCaptchaControl(Layout layout, BasicFormElement element)
	{
		captcha = new CaptchaComponent(msg, form.getCaptchaLength(), form.getLayoutSettings().isCompactInputs());
		layout.addComponent(HtmlTag.br());
		layout.addComponent(captcha.getAsComponent());
		return true;
	}

	private boolean createRegistrationCodeControl(Layout layout, BasicFormElement element)
	{
		registrationCode = new TextField(msg.getMessage("RegistrationRequest.registrationCode"));
		registrationCode.setRequiredIndicatorVisible(true);
		layout.addComponent(registrationCode);
		return true;
	}
	
	@Override
	protected boolean isPolicyAgreementsIsFiltered(PolicyAgreementConfiguration toCheck)
	{
		return false;
	}
	
	RegistrationForm getForm()
	{
		return form;
	}
	
	RemotelyAuthenticatedContext getRemoteAuthnContext()
	{
		return remotelyAuthenticated;
	}

	private class RegGridAuthnPanelFactory implements AuthNPanelFactory
	{
		@Override
		public FirstFactorAuthNPanel createRegularAuthnPanel(AuthNOption authnOption)
		{
			return null;
		}

		@Override
		public FirstFactorAuthNPanel createGridCompatibleAuthnPanel(AuthNOption authnOption)
		{
			AuthenticationOptionKey optionId = new AuthenticationOptionKey(
					authnOption.authenticator.getAuthenticatorId(),
					authnOption.authenticatorUI.getId());

			FirstFactorAuthNPanel authNPanel = new FirstFactorAuthNPanel(msg, null, null, null, true,
					authnOption.authenticatorUI, optionId);

			if (signUpAuthNController != null)
			{
				authnOption.authenticatorUI.setAuthenticationCallback(
						signUpAuthNController.buildCallback(authnOption));
			}

			return authNPanel;
		}
	}
}
