/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.ui.UI;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext.FactorOrder;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.authn.LoginMachineDetailsExtractor;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Collects authN results from the 2nd authenticator. Afterwards, the final authentication result 
 * processing is launched.
 */
class SecondFactorAuthNResultCallback implements AuthenticationCallback
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			SecondFactorAuthNResultCallback.class);
	private final MessageSource msg;
	private final InteractiveAuthenticationProcessor authnProcessor;
	private final AuthenticationRealm realm;
	private final AuthenticationListener authNListener;
	private final Supplier<Boolean> rememberMeProvider;
	private final PartialAuthnState partialState;
	private final SecondFactorAuthNPanel authNPanel;

	private String clientIp;


	SecondFactorAuthNResultCallback(MessageSource msg,
			InteractiveAuthenticationProcessor authnProcessor, AuthenticationRealm realm,
			AuthenticationListener authNListener, Supplier<Boolean> rememberMeProvider,
			PartialAuthnState partialState,
			SecondFactorAuthNPanel authNPanel)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.realm = realm;
		this.authNListener = authNListener;
		this.rememberMeProvider = rememberMeProvider;
		this.partialState = partialState;
		this.authNPanel = authNPanel;
	}

	@Override
	public void onCompletedAuthentication(AuthenticationResult result)
	{
		processAuthn(result);
	}
	
	private void processAuthn(AuthenticationResult result)
	{
		log.trace("Received authentication result of the 2nd authenticator" + result);
		AuthenticationStepContext stepContext = new AuthenticationStepContext(realm, 
				partialState.getAuthenticationFlow(), 
				authNPanel.getAuthenticationOptionId(), FactorOrder.SECOND, null);
		VaadinServletRequest servletRequest = VaadinServletRequest.getCurrent();
		VaadinServletResponse servletResponse = VaadinServletResponse.getCurrent();
		LoginMachineDetails loginMachineDetails = LoginMachineDetailsExtractor
				.getLoginMachineDetailsFromCurrentRequest();
		PostAuthenticationStepDecision postSecondFactorDecision = authnProcessor.processSecondFactorResult(
				partialState, result, stepContext, 
				loginMachineDetails, isSetRememberMe(), servletRequest, servletResponse);
		switch (postSecondFactorDecision.getDecision())
		{
		case COMPLETED:
			log.trace("Authentication completed");
			setAuthenticationCompleted();
			return;
		case ERROR:
			log.trace("Authentication failed ");
			handleError(postSecondFactorDecision.getErrorDetail().error.resovle(msg));
			switchToPrimaryAuthentication();
		case GO_TO_2ND_FACTOR:
			log.error("2nd factor required after 2nd factor? {}", result);
			throw new IllegalStateException("authentication error");
		case UNKNOWN_REMOTE_USER:
			log.error("unknown remote user after 2nd factor? {}", result);
			throw new IllegalStateException("authentication error");
		default:
			throw new IllegalStateException("Unknown authn decision: " + postSecondFactorDecision.getDecision());
		}
	}
	
	@Override
	public void onStartedAuthentication()
	{
		clientIp = HTTPRequestContext.getCurrent().getClientIP();
		if (authNListener != null)
			authNListener.authenticationStarted();
	}

	@Override
	public void onCancelledAuthentication()
	{
		setAuthenticationAborted();
	}

	@Override
	public boolean isSetRememberMe()
	{
		return rememberMeProvider.get();
	}

	@Override
	public PartialAuthnState getPostFirstFactorAuthnState()
	{
		return partialState;
	}
	
	private void handleError(String errorToShow)
	{
		setAuthenticationAborted();
		authNPanel.focusIfPossible();
		NotificationPopup.showError(errorToShow, "");
		authNPanel.showWaitScreenIfNeeded(clientIp);
	}
	
	/**
	 * Resets the authentication UI to the initial state
	 */
	private void switchToPrimaryAuthentication()
	{
		if (authNListener != null)
			authNListener.switchBackToFirstFactor();
	}
	
	private void setAuthenticationAborted()
	{
		if (authNListener != null)
			authNListener.authenticationAborted();
	}
	
	private void setAuthenticationCompleted()
	{
		if (authNListener != null)
			authNListener.authenticationCompleted();
		UI ui = UI.getCurrent();
		if (ui == null)
		{
			log.error("BUG Can't get UI to redirect the authenticated user.");
			throw new IllegalStateException("AuthenticationProcessor.authnInternalError");
		}
		ui.getPage().reload();
	}

	/**
	 * Used by upstream code holding this component to be informed about changes in this component. 
	 */
	public interface AuthenticationListener
	{
		void authenticationStarted();
		void authenticationAborted();
		void authenticationCompleted();
		void switchBackToFirstFactor();
	}
}