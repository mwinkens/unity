/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_SHOW_CANCEL;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationOption;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.AuthenticationOptionKeyUtils;
import pl.edu.icm.unity.webui.authn.AuthenticationScreen;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.remote.UnknownUserDialog;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Organizes authentication options in columns, making them instantly usable.
 * 
 * @author K. Benedyczak
 */
public class ColumnInstantAuthenticationScreen extends CustomComponent implements AuthenticationScreen
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ColumnInstantAuthenticationScreen.class);
	private final UnityMessageSource msg;
	private final VaadinEndpointProperties config;
	private final ResolvedEndpoint endpointDescription;
	private final Supplier<Boolean> outdatedCredentialDialogLauncher;
	private final Runnable registrationDialogLauncher;
	private final boolean enableRegistration;
	private final CancelHandler cancelHandler;
	
	private final EntityManagement idsMan;
	private final ExecutorsService execService;
	private final Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider;
	private final WebAuthenticationProcessor authnProcessor;	
	private final LocaleChoiceComponent localeChoice;
	private final List<AuthenticationOption> authenticators;
	
	private AuthenticationOptionsHandler authnOptionsHandler;
	private PrimaryAuthNPanel authNPanelInProgress;
	private CheckBox rememberMe;
	private RemoteAuthenticationProgress authNProgress;
	private AuthnOptionsColumns authNColumns;
	private VerticalLayout secondFactorHolder;
	
	public ColumnInstantAuthenticationScreen(UnityMessageSource msg, VaadinEndpointProperties config,
			ResolvedEndpoint endpointDescription,
			Supplier<Boolean> outdatedCredentialDialogLauncher,
			Runnable registrationDialogLauncher, CancelHandler cancelHandler,
			EntityManagement idsMan,
			ExecutorsService execService, boolean enableRegistration,
			Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider,
			WebAuthenticationProcessor authnProcessor,
			LocaleChoiceComponent localeChoice,
			List<AuthenticationOption> authenticators)
	{
		this.msg = msg;
		this.config = config;
		this.endpointDescription = endpointDescription;
		this.outdatedCredentialDialogLauncher = outdatedCredentialDialogLauncher;
		this.registrationDialogLauncher = registrationDialogLauncher;
		this.cancelHandler = cancelHandler;
		this.idsMan = idsMan;
		this.execService = execService;
		this.enableRegistration = enableRegistration;
		this.unknownUserDialogProvider = unknownUserDialogProvider;
		this.authnProcessor = authnProcessor;
		this.localeChoice = localeChoice;
		this.authenticators = authenticators;
		
		init();
	}

	@Override
	public void refresh(VaadinRequest request) 
	{
		log.info("Refresh called");
		refreshAuthenticationState(request);
		authNColumns.focusFirst();
	}
	
	protected void init()
	{
		this.authnOptionsHandler = new AuthenticationOptionsHandler(authenticators);
		
		VerticalLayout topLevelLayout = new VerticalLayout();
		topLevelLayout.setMargin(new MarginInfo(false, true, false, true));
		topLevelLayout.setHeightUndefined();
		setCompositionRoot(topLevelLayout);

		Component languageChoice = getLanguageChoiceComponent();
		topLevelLayout.addComponent(languageChoice);
		topLevelLayout.setComponentAlignment(languageChoice, Alignment.TOP_CENTER);
		
		authNProgress = new RemoteAuthenticationProgress(msg, this::triggerAuthNCancel);
		topLevelLayout.addComponent(authNProgress);
		authNProgress.setInternalVisibility(false);
		topLevelLayout.setComponentAlignment(authNProgress, Alignment.TOP_RIGHT);
		
		Component authnOptionsComponent = getAuthenticationComponent();
		topLevelLayout.addComponent(authnOptionsComponent);
		topLevelLayout.setComponentAlignment(authnOptionsComponent, Alignment.MIDDLE_CENTER);
		
		if (outdatedCredentialDialogLauncher.get())
			return;
		
		//Extra safety - it can happen that we entered the UI in pipeline of authentication,
		// if this UI expired in the meantime. Shouldn't happen often as heart of authentication UI
		// is beating very slowly but in case of very slow user we may still need to refresh.
		refreshAuthenticationState(VaadinService.getCurrentRequest());
	}
	
	private Component getLanguageChoiceComponent()
	{
		HorizontalLayout languageChoiceLayout = new HorizontalLayout();
		languageChoiceLayout.setMargin(true);
		languageChoiceLayout.setSpacing(false);
		languageChoiceLayout.setWidth(100, Unit.PERCENTAGE);
		languageChoiceLayout.addComponent(localeChoice);
		languageChoiceLayout.setComponentAlignment(localeChoice, Alignment.MIDDLE_RIGHT);
		return languageChoiceLayout;
	}
	

	/**
	 * @return main authentication: logo, title, columns with authentication options
	 */
	private Component getAuthenticationComponent()
	{
		VerticalLayout authenticationMainLayout = new VerticalLayout();
		authenticationMainLayout.setMargin(false);
		
		String logoURL = config.getValue(VaadinEndpointProperties.AUTHN_LOGO);
		if (!logoURL.isEmpty())
		{
			Resource logoResource = ImageUtils.getConfiguredImageResource(logoURL);
			Image image = new Image(null, logoResource);
			image.addStyleName("u-authn-logo");
			authenticationMainLayout.addComponent(image);
			authenticationMainLayout.setComponentAlignment(image, Alignment.TOP_CENTER);
		}
		
		Component title = getTitleComponent();
		if (title != null)
		{
			authenticationMainLayout.addComponent(title);
			authenticationMainLayout.setComponentAlignment(title, Alignment.TOP_CENTER);
		}
		
		//TODO search support
		
		authNColumns = new AuthnOptionsColumns(config, msg, 
				authnOptionsHandler, enableRegistration, new AuthnPanelFactoryImpl(), 
				registrationDialogLauncher);
		authenticationMainLayout.addComponent(authNColumns);
		authenticationMainLayout.setComponentAlignment(authNColumns, Alignment.TOP_CENTER);
		
		secondFactorHolder = new VerticalLayout();
		secondFactorHolder.setMargin(false);
		authenticationMainLayout.addComponent(secondFactorHolder);
		authenticationMainLayout.setComponentAlignment(secondFactorHolder, Alignment.TOP_CENTER);
		secondFactorHolder.setVisible(false);
		
		AuthenticationRealm realm = endpointDescription.getRealm();
		if (realm.getAllowForRememberMeDays() > 0)
		{
			Component rememberMe = getRememberMeComponent(realm); 
			authenticationMainLayout.addComponent(rememberMe);
		}
		
		if (cancelHandler != null && config.getBooleanValue(AUTHN_SHOW_CANCEL))
		{
			authenticationMainLayout.addComponent(getCancelComponent());
		}
		
		return authenticationMainLayout;
	}
	
	private Component getCancelComponent()
	{
		Button cancel = new Button(msg.getMessage("AuthenticationUI.cancelAuthentication"));
		cancel.addStyleName(Styles.vButtonLink.toString());
		cancel.addClickListener(event -> {
			if (authNPanelInProgress != null)
				authNPanelInProgress.cancel();
			cancelHandler.onCancel();
		});
		HorizontalLayout bottomWrapper = new HorizontalLayout();
		bottomWrapper.setMargin(true);
		bottomWrapper.setWidth(100, Unit.PERCENTAGE);
		bottomWrapper.addComponent(cancel);
		bottomWrapper.setComponentAlignment(cancel, Alignment.TOP_CENTER);
		return bottomWrapper;
	}
	
	private Component getRememberMeComponent(AuthenticationRealm realm)
	{
		HorizontalLayout bottomWrapper = new HorizontalLayout();
		bottomWrapper.setMargin(true);
		bottomWrapper.setWidth(100, Unit.PERCENTAGE);
		rememberMe = new CheckBox(msg.getMessage("AuthenticationUI.rememberMe", 
				realm.getAllowForRememberMeDays()));
		rememberMe.addStyleName("u-authn-rememberMe");
		bottomWrapper.addComponent(rememberMe);
		bottomWrapper.setComponentAlignment(rememberMe, Alignment.TOP_CENTER);
		return bottomWrapper;
	}
	
	private Component getTitleComponent()
	{
		String configuredMainTitle = config.getLocalizedValue(VaadinEndpointProperties.AUTHN_TITLE, msg.getLocale());
		String mainTitle = null;
		String serviceName = endpointDescription.getEndpoint().getConfiguration().getDisplayedName().getValue(msg);

		if (configuredMainTitle != null && !configuredMainTitle.isEmpty())
		{
			mainTitle = String.format(configuredMainTitle, serviceName);
		} else if (configuredMainTitle == null)
		{
			mainTitle = msg.getMessage("AuthenticationUI.login", serviceName);
		}
		if (mainTitle != null)
		{
			Label mainTitleLabel = new Label(mainTitle);
			mainTitleLabel.addStyleName("u-authn-title");
			return mainTitleLabel;
		}
		return null;
	}
	
	private PrimaryAuthNPanel buildBaseAuthenticationOptionWidget(AuthenticationOption authNOption, 
			VaadinAuthenticationUI vaadinAuthenticationUI, boolean gridCompatible)
	{
		PrimaryAuthNPanel authNPanel = new PrimaryAuthNPanel(msg, authnProcessor, 
				execService, cancelHandler, 
				endpointDescription.getRealm(),
				endpointDescription.getEndpoint().getContextAddress(), 
				unknownUserDialogProvider,
				this::isSetRememberMe,
				gridCompatible);
		authNPanel.setAuthenticationListener(new PrimaryAuthenticationListenerImpl(authNPanel));
		String optionId = AuthenticationOptionKeyUtils.encode(authNOption.getId(), vaadinAuthenticationUI.getId()); 
		authNPanel.setAuthenticator(vaadinAuthenticationUI, authNOption, optionId);
		return authNPanel;
	}

	private SecondFactorAuthNPanel build2ndFactorAuthenticationOptionWidget(
			VaadinAuthenticationUI vaadinAuthenticationUI, PartialAuthnState partialAuthnState)
	{
		SecondFactorAuthNPanel authNPanel = new SecondFactorAuthNPanel(msg, authnProcessor, idsMan,
				execService, cancelHandler, 
				endpointDescription.getRealm(),
				unknownUserDialogProvider,
				this::isSetRememberMe);
		authNPanel.setAuthenticationListener(new SecondaryAuthenticationListenerImpl());
		authNPanel.setAuthenticator(vaadinAuthenticationUI, partialAuthnState);
		return authNPanel;
	}

	
	private boolean isSetRememberMe()
	{
		return rememberMe != null && rememberMe.getValue();
	}

	private void refreshAuthenticationState(VaadinRequest request) 
	{
		if (authNPanelInProgress != null)
			authNPanelInProgress.refresh(request);
	}

	private void triggerAuthNCancel() 
	{
		if (authNPanelInProgress != null)
			authNPanelInProgress.cancel();
		onStoppedAuthentication();
	}

	private void onStoppedAuthentication()
	{
		authNColumns.enableAll();
		authNProgress.setInternalVisibility(false);
		authNPanelInProgress = null;
	}
	
	private void switchToSecondaryAuthentication(PartialAuthnState partialState)
	{
		VaadinAuthentication secondaryAuthn = (VaadinAuthentication) partialState.getSecondaryAuthenticator();
		Collection<VaadinAuthenticationUI> secondaryAuthnUIs = secondaryAuthn.createUIInstance();
		if (secondaryAuthnUIs.size() > 1)
		{
			log.warn("Configuration error: the authenticator configured as the second "
					+ "factor " + secondaryAuthn.getAuthenticatorId() + 
					" provides multiple authentication possibilities. "
					+ "This is unsupported currently, "
					+ "use this authenticator as the first factor only. "
					+ "The first possibility will be used, "
					+ "but in most cases it is not what you want.");
		}
		VaadinAuthenticationUI secondaryUI = secondaryAuthnUIs.iterator().next();
		
		authNColumns.setVisible(false);
		
		SecondFactorAuthNPanel authNPanel = build2ndFactorAuthenticationOptionWidget(secondaryUI, partialState);
		AuthnOptionsColumn wrapping2ndFColumn = new AuthnOptionsColumn(null, 
				VaadinEndpointProperties.DEFAULT_AUTHN_COLUMN_WIDTH);
		wrapping2ndFColumn.addOptions(Lists.newArrayList(new AuthnOptionsColumn.ComponentWithId("", authNPanel)));
		secondFactorHolder.removeAllComponents();
		Label mfaInfo = new Label(msg.getMessage("AuthenticationUI.mfaRequired"));
		mfaInfo.addStyleName(Styles.error.toString());
		wrapping2ndFColumn.focusFirst();
		secondFactorHolder.addComponent(mfaInfo);
		secondFactorHolder.setComponentAlignment(mfaInfo, Alignment.TOP_CENTER);
		secondFactorHolder.addComponent(wrapping2ndFColumn);
		wrapping2ndFColumn.setWidthUndefined();
		secondFactorHolder.setComponentAlignment(wrapping2ndFColumn, Alignment.TOP_CENTER);
		secondFactorHolder.setVisible(true);
	}
	
	private void switchBackToPrimaryAuthentication()
	{
		authNColumns.setVisible(true);
		authNColumns.enableAll();
		authNColumns.focusFirst();
		secondFactorHolder.removeAllComponents();
		secondFactorHolder.setVisible(false);
	}
	
	private class AuthnPanelFactoryImpl implements AuthNPanelFactory
	{
		@Override
		public PrimaryAuthNPanel createRegularAuthnPanel(AuthenticationOption option, VaadinAuthenticationUI ui)
		{
			return buildBaseAuthenticationOptionWidget(option, ui, false);
		}

		@Override
		public PrimaryAuthNPanel createGridCompatibleAuthnPanel(AuthenticationOption option,
				VaadinAuthenticationUI ui)
		{
			return buildBaseAuthenticationOptionWidget(option, ui, true);
		}
	}
	
	private class PrimaryAuthenticationListenerImpl implements PrimaryAuthNPanel.AuthenticationListener
	{
		private final PrimaryAuthNPanel authNPanel;
		
		PrimaryAuthenticationListenerImpl(PrimaryAuthNPanel authNPanel)
		{
			this.authNPanel = authNPanel;
		}

		@Override
		public void authenticationStarted(boolean showProgress)
		{
			authNPanelInProgress = authNPanel;
			authNProgress.setInternalVisibility(showProgress);
			authNColumns.disableAllExcept(authNPanel.getAuthenticationOptionId());
		}

		@Override
		public void authenticationStopped()
		{
			onStoppedAuthentication();
		}

		@Override
		public void switchTo2ndFactor(PartialAuthnState partialState)
		{
			switchToSecondaryAuthentication(partialState);
		}
	}
	
	private class SecondaryAuthenticationListenerImpl implements SecondFactorAuthNPanel.AuthenticationListener
	{
		@Override
		public void switchBackToFirstFactor()
		{
			switchBackToPrimaryAuthentication();
		}
	}
}
