/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms.enquiry;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.FormPrefill;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.finalization.WorkflowCompletedComponent;
import pl.edu.icm.unity.webui.forms.FormsUIHelper;
import pl.edu.icm.unity.webui.forms.InvitationResolver;
import pl.edu.icm.unity.webui.forms.PrefilledSet;
import pl.edu.icm.unity.webui.forms.RegCodeException;
import pl.edu.icm.unity.webui.forms.RegCodeException.ErrorCause;
import pl.edu.icm.unity.webui.forms.ResolvedInvitationParam;
import pl.edu.icm.unity.webui.forms.StandalonePublicView;
import pl.edu.icm.unity.webui.forms.URLQueryPrefillCreator;
import pl.edu.icm.unity.webui.forms.reg.GetRegistrationCodeDialog;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormDialogProvider;

/**
 * Provides public enquiry view. Used for enquiry invitation flow.
 * 
 * @author P.Piernik
 */
@PrototypeComponent
public class StandalonePublicEnquiryView extends CustomComponent implements StandalonePublicView
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, StandalonePublicEnquiryView.class);
	
	private MessageSource msg;
	private ImageAccessService imageAccessService;
	
	private VerticalLayout main;
	private String registrationCode;
	private EnquiryResponseEditorController editorController;
	private InvitationResolver invitationResolver;
	private PostFillingHandler postFillHandler;

	private EnquiryForm form;
	private EnquiryResponseEditor editor;
	private ResolvedInvitationParam invitation;

	private final URLQueryPrefillCreator urlQueryPrefillCreator;
	
	
	@Autowired
	public StandalonePublicEnquiryView(EnquiryResponseEditorController editorController,
			InvitationResolver invitationResolver, MessageSource msg, 
			ImageAccessService imageAccessService, URLQueryPrefillCreator urlQueryPrefillCreator)
	{
		this.editorController = editorController;
		this.urlQueryPrefillCreator = urlQueryPrefillCreator;
		this.invitationResolver = invitationResolver;
		this.msg = msg;
		this.imageAccessService = imageAccessService;
	}

	@Override
	public String getFormName()
	{
		if (form == null)
			return null;
		return form.getName();
	}

	public StandalonePublicEnquiryView init(EnquiryForm form)
	{
		this.form = form;
		String pageTitle = form.getPageTitle() == null ? null : form.getPageTitle().getValue(msg);
		this.postFillHandler = new PostFillingHandler(form.getName(), form.getWrapUpConfig(), msg, pageTitle,
				form.getLayoutSettings().getLogoURL(), false);
		return this;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		initUIBase();
		if (registrationCode == null)
			registrationCode = RegistrationFormDialogProvider.getCodeFromURL();

		if (registrationCode == null)
		{
			askForCode(() -> doShowEditorOrSkipToFinalStep());
		} else
		{
			doShowEditorOrSkipToFinalStep();
		}
	}

	private void doShowEditorOrSkipToFinalStep()
	{
		
		try
		{
			invitation = invitationResolver.getInvitationByCode(registrationCode, form);
		} catch (RegCodeException e)
		{
			log.error("Can not get invitation", e);
			handleError(e, e.cause);
			return;
		} 
		
		
		EnquiryInvitationParam enqInvitation = invitation.getAsEnquiryInvitationParam();
		
		try
		{
			PrefilledSet currentUserData = editorController.getPrefilledSetForSticky(form, 
					new EntityParam(enqInvitation.getEntity()));
			PrefilledSet prefilled = mergeInvitationAndCurrentUserData(enqInvitation, currentUserData, form);
			prefilled = prefilled.mergeWith(urlQueryPrefillCreator.create(form));
			
			editor = editorController.getEditorInstanceForUnauthenticatedUser(form,
					enqInvitation.getFormPrefill().getMessageParamsWithCustomVarObject(
							MessageTemplateDefinition.CUSTOM_VAR_PREFIX),
					RemotelyAuthenticatedPrincipal.getLocalContext(), prefilled,
					new EntityParam(enqInvitation.getEntity()));

		} catch (Exception e)
		{
			log.error("Can not setup enquiry editor", e);
			handleError(e, ErrorCause.MISCONFIGURED);
			return;
		}
	
		showEditorContent(editor);
	}

	private PrefilledSet mergeInvitationAndCurrentUserData(EnquiryInvitationParam invitation, PrefilledSet fromUser, 
			EnquiryForm form)
	{

		FormPrefill formPrefill = invitation.getFormPrefill();
		return new PrefilledSet(formPrefill.getIdentities(),
				mergePreffiledGroups(formPrefill.getAllowedGroups(), formPrefill.getGroupSelections(), fromUser.groupSelections, form),
				mergePreffiledAttributes(formPrefill.getAttributes(), fromUser.attributes),
				formPrefill.getAllowedGroups());
	}

	private Map<Integer, PrefilledEntry<Attribute>> mergePreffiledAttributes(
			Map<Integer, PrefilledEntry<Attribute>> fromInvitation,
			Map<Integer, PrefilledEntry<Attribute>> fromUser)
	{
		Map<Integer, PrefilledEntry<Attribute>> mergedAttributes = new HashMap<>();

		if (fromUser.isEmpty())
		{
			return fromInvitation;
		}

		for (Entry<Integer, PrefilledEntry<Attribute>> entryFromUser : fromUser.entrySet())
		{
			PrefilledEntry<Attribute> fromInvitationAttr = fromInvitation.get(entryFromUser.getKey());
			if (fromInvitationAttr != null)
			{
				mergedAttributes.put(entryFromUser.getKey(), fromInvitationAttr);
			} else
			{
				mergedAttributes.put(entryFromUser.getKey(), entryFromUser.getValue());
			}
		}
		return mergedAttributes;
	}

	private Map<Integer, PrefilledEntry<GroupSelection>> mergePreffiledGroups(Map<Integer, GroupSelection> allowedFromInvitiation,
			Map<Integer, PrefilledEntry<GroupSelection>> fromInvitation,
			Map<Integer, PrefilledEntry<GroupSelection>> fromUser, EnquiryForm form)
	{
		
		
		Map<Integer, PrefilledEntry<GroupSelection>> mergedGroups = new HashMap<>();
		
		if (fromUser.isEmpty())
		{
			return fromInvitation;	
		}
	
		for (Map.Entry<Integer, PrefilledEntry<GroupSelection>> entryFromUser : fromUser.entrySet())
		{
			PrefilledEntry<GroupSelection> fromInvitationG = fromInvitation.get(entryFromUser.getKey());

			if (fromInvitationG == null)
			{
				mergedGroups.put(entryFromUser.getKey(),entryFromUser.getValue());
				continue;
			}

			if (fromInvitationG.getMode().isInteractivelyEntered())
			{
				Set<String> mergedSet = new LinkedHashSet<>(
						fromInvitationG.getEntry().getSelectedGroups());
				mergedSet.addAll(entryFromUser.getValue().getEntry().getSelectedGroups());
				mergedGroups.put(entryFromUser.getKey(), new PrefilledEntry<GroupSelection>(new GroupSelection(
						mergedSet.stream().collect(Collectors.toList())),
						entryFromUser.getValue().getMode()));
			} else
			{
				mergedGroups.put(entryFromUser.getKey(), fromInvitationG);
			}

		}
		return mergedGroups;
	}

	private void showEditorContent(EnquiryResponseEditor editor)
	{
		main.addComponent(editor);
		editor.setWidth(100, Unit.PERCENTAGE);
		main.setComponentAlignment(editor, Alignment.MIDDLE_CENTER);
		Component buttonsBar = createButtonsBar();
		main.addComponent(buttonsBar);
		main.setComponentAlignment(buttonsBar, Alignment.MIDDLE_CENTER);
	}

	private void askForCode(Runnable uiCreator)
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
						cancel();
					}
				}, msg.getMessage("GetEnquiryCodeDialog.title"),
				msg.getMessage("GetEnquiryCodeDialog.information"),
				msg.getMessage("GetEnquiryCodeDialog.code"));
		askForCodeDialog.show();
	}

	private void initUIBase()
	{
		if (form.getPageTitle() != null)
			Page.getCurrent().setTitle(form.getPageTitle().getValue(msg));
		main = new VerticalLayout();
		addStyleName("u-standalone-public-form");
		setCompositionRoot(main);
		setWidth(100, Unit.PERCENTAGE);
	}

	private Component createButtonsBar()
	{
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setWidth(editor.formWidth(), editor.formWidthUnit());

		Button okButton = FormsUIHelper.createOKButton(
				msg.getMessage("RegistrationRequestEditorDialog.submitRequest"), event -> {
					WorkflowFinalizationConfiguration config = submit(form, editor);
					gotoFinalStep(config);
				});

		Button cancelButton = FormsUIHelper.createCancelButton(msg.getMessage("cancel"), event -> {
			WorkflowFinalizationConfiguration config = cancel();
			gotoFinalStep(config);
		});

		buttons.addComponents(cancelButton, okButton);
		buttons.setSpacing(true);
		buttons.setMargin(false);
		return buttons;
	}

	private void handleError(Exception e, ErrorCause cause)
	{
		WorkflowFinalizationConfiguration finalScreenConfig = postFillHandler
				.getFinalRegistrationConfigurationOnError(cause.getTriggerState());
		gotoFinalStep(finalScreenConfig);
	}

	private void gotoFinalStep(WorkflowFinalizationConfiguration config)
	{
		if (config == null)
			return;
		if (config.autoRedirect)
			redirect(Page.getCurrent(), config.redirectURL);
		else
			showFinalScreen(config);
	}

	private void showFinalScreen(WorkflowFinalizationConfiguration config)
	{
		log.debug("Enquiry is finalized, status: {}", config);
		WorkflowCompletedComponent finalScreen = new WorkflowCompletedComponent(config, this::redirect, imageAccessService);
		Component wrapper = finalScreen.getWrappedForFullSizeComponent();
		setCompositionRoot(wrapper);
		setSizeFull();
	}

	private void redirect(Page page, String redirectUrl)
	{
		log.debug("Enquiry is finalized, redirecting to: {}", redirectUrl);
		page.open(redirectUrl, null);
	}
	
	private WorkflowFinalizationConfiguration submit(EnquiryForm form, EnquiryResponseEditor editor)
	{
		EnquiryResponse request = editor.getRequestWithStandardErrorHandling(true).orElse(null);
		if (request == null)
			return null;
		request.setRegistrationCode(registrationCode);
		try
		{
			return editorController.submitted(request, form, TriggeringMode.manualStandalone,
					invitation == null ? Optional.empty()
							: Optional.of(new RewriteComboToEnquiryRequest(invitation.code, invitation.entity, form)));
		} catch (WrongArgumentException e)
		{
			FormsUIHelper.handleFormSubmissionError(e, msg, editor);
			return null;
		}
	}
	
	private WorkflowFinalizationConfiguration cancel()
	{	
		return postFillHandler.getFinalRegistrationConfigurationOnError(
				TriggeringState.CANCELLED);
	}
}
