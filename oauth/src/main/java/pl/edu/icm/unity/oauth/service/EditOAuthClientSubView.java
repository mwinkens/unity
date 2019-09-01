/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.nimbusds.oauth2.sdk.client.ClientType;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.TextFieldWithChangeConfirmation;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;
import pl.edu.icm.unity.webui.common.file.ImageField;
import pl.edu.icm.unity.webui.common.webElements.UnitySubView;

/**
 * Subview for edit oauth client.
 * 
 * @author P.Piernik
 *
 */
class EditOAuthClientSubView extends CustomComponent implements UnitySubView
{
	private UnityMessageSource msg;
	private URIAccessService uriAccessService;
	private UnityServerConfiguration serverConfig;
	private Binder<OAuthClient> binder;
	private boolean editMode = false;
	private Set<String> allClientsIds;

	EditOAuthClientSubView(UnityMessageSource msg, URIAccessService uriAccessService,
			UnityServerConfiguration serverConfig, Set<String> allClientsIds, OAuthClient toEdit,
			Consumer<OAuthClient> onConfirm, Runnable onCancel)
	{
		this.msg = msg;
		this.uriAccessService = uriAccessService;
		this.serverConfig = serverConfig;
		this.allClientsIds = allClientsIds;

		editMode = toEdit != null;
		binder = new Binder<>(OAuthClient.class);
		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(buildHeaderSection());
		mainView.addComponent(buildConsentScreenSection());
		Runnable onConfirmR = () -> {
			try
			{
				onConfirm.accept(getOAuthClient());
			} catch (FormValidationException e)
			{
				NotificationPopup.showError(msg,
						msg.getMessage("EditOAuthClientSubView.invalidConfiguration"), e);
			}
		};
		mainView.addComponent(editMode
				? StandardButtonsHelper.buildConfirmEditButtonsBar(msg, onConfirmR, onCancel)
				: StandardButtonsHelper.buildConfirmNewButtonsBar(msg, onConfirmR, onCancel));

		binder.setBean(editMode ? toEdit.clone()
				: new OAuthClient(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
		setCompositionRoot(mainView);
	}

	private FormLayoutWithFixedCaptionWidth buildHeaderSection()
	{
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);

		TextField name = new TextField();
		name.setCaption(msg.getMessage("EditOAuthClientSubView.name"));
		binder.forField(name).bind("name");
		header.addComponent(name);

		TextField id = new TextField();
		id.setCaption(msg.getMessage("EditOAuthClientSubView.id"));
		id.setReadOnly(editMode);
		id.setWidth(30, Unit.EM);
		binder.forField(id).asRequired(msg.getMessage("fieldRequired")).withValidator((v, c) -> {
			if (v != null && allClientsIds.contains(v))
			{
				return ValidationResult.error(msg.getMessage("EditOAuthClientSubView.invalidClientId"));
			}

			return ValidationResult.ok();
		}).bind("id");
		header.addComponent(id);

		if (!editMode)
		{
			TextField secret = new TextField();
			secret.setCaption(msg.getMessage("EditOAuthClientSubView.secret"));
			secret.setWidth(30, Unit.EM);
			binder.forField(secret).asRequired(msg.getMessage("fieldRequired")).bind("secret");
			header.addComponent(secret);

		} else
		{
			TextFieldWithChangeConfirmation secret = new TextFieldWithChangeConfirmation(msg);
			secret.setCaption(msg.getMessage("EditOAuthClientSubView.secret"));
			secret.setWidth(30, Unit.EM);
			binder.forField(secret).withValidator((v, c) -> {
				if (secret.isEditMode())
				{
					return ValidationResult.error(msg.getMessage("fieldRequired"));
				}

				return ValidationResult.ok();
			}).bind("secret");
			header.addComponent(secret);
		}

		ChipsWithDropdown<String> allowedFlows = new ChipsWithDropdown<>();
		allowedFlows.setCaption(msg.getMessage("EditOAuthClientSubView.allowedFlows"));
		allowedFlows.setItems(
				Stream.of(GrantFlow.values()).map(f -> f.toString()).collect(Collectors.toList()));
		binder.forField(allowedFlows).withValidator((v, c) -> {
			if (v == null || v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			}

			return ValidationResult.ok();
		}).bind("flows");
		header.addComponent(allowedFlows);

		ComboBox<String> type = new ComboBox<>();
		type.setCaption(msg.getMessage("EditOAuthClientSubView.type"));
		type.setItems(Stream.of(ClientType.values()).map(f -> f.toString()).collect(Collectors.toList()));
		type.setEmptySelectionAllowed(false);
		binder.forField(type).bind("type");
		header.addComponent(type);

		ChipsWithTextfield redirectURIs = new ChipsWithTextfield(msg);
		redirectURIs.setCaption(msg.getMessage("EditOAuthClientSubView.authorizedRedirectURIs"));
		binder.forField(redirectURIs).bind("redirectURIs");
		header.addComponent(redirectURIs);

		return header;
	}

	private Component buildConsentScreenSection()
	{
		FormLayoutWithFixedCaptionWidth consentScreenL = new FormLayoutWithFixedCaptionWidth();
		consentScreenL.setMargin(false);

		TextField title = new TextField();
		title.setCaption(msg.getMessage("EditOAuthClientSubView.title"));
		binder.forField(title).bind("title");
		consentScreenL.addComponent(title);

		ImageField logo = new ImageField(msg, uriAccessService, serverConfig.getFileSizeLimit());
		logo.setCaption(msg.getMessage("EditOAuthProviderSubView.logo"));
		logo.configureBinding(binder, "logo");
		consentScreenL.addComponent(logo);

		CollapsibleLayout consentScreenSection = new CollapsibleLayout(
				msg.getMessage("EditOAuthClientSubView.consentScreen"), consentScreenL);
		consentScreenSection.expand();
		return consentScreenSection;
	}

	private OAuthClient getOAuthClient() throws FormValidationException
	{
		if (binder.validate().hasErrors())
			throw new FormValidationException();

		return binder.getBean();
	}

	@Override
	public List<String> getBredcrumbs()
	{
		if (editMode)
			return Arrays.asList(msg.getMessage("EditOAuthClientSubView.client"), binder.getBean().getId());
		else
			return Arrays.asList(msg.getMessage("EditOAuthClientSubView.newClient"));
	}

}
