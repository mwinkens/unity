/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.finalization;

/**
 * Complete information on what to show on the final screen after completed workflow.
 * @author K. Benedyczak
 */
public class WorkflowFinalizationConfiguration
{
	public final boolean success;
	public final boolean autoRedirect;
	public final String pageTitle;
	public final String logoURL;
	public final String mainInformation;
	public final String extraInformation;
	public final String redirectURL;
	public final String redirectButtonText;
	public final int redirectAfterTime;
	private boolean isAutoLoginAfterSignUp;

	public WorkflowFinalizationConfiguration(boolean success, boolean autoRedirect, String pageTitle, String logoURL,
			String mainInformation, String extraInformation, String redirectURL, String redirectButtonText, int redirectAfterTime)
	{
		this.success = success;
		this.autoRedirect = autoRedirect;
		this.pageTitle = pageTitle;
		this.logoURL = logoURL;
		this.mainInformation = mainInformation;
		this.extraInformation = extraInformation;
		this.redirectURL = redirectURL;
		this.redirectButtonText = redirectButtonText;
		this.redirectAfterTime = redirectAfterTime;
	}
	
	public boolean isAutoLoginAfterSignUp()
	{
		return isAutoLoginAfterSignUp;
	}

	public void setAutoLoginAfterSignUp(boolean isAutoLoginAfterSignUp)
	{
		this.isAutoLoginAfterSignUp = isAutoLoginAfterSignUp;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static WorkflowFinalizationConfiguration autoRedirect(String redirectURL)
	{
		return new Builder().setAutoRedirect(true).setRedirectURL(redirectURL).build();
	}

	public static WorkflowFinalizationConfiguration basicError(String mainInfo, String redirectURL)
	{
		return new Builder().setSuccess(false).setMainInformation(mainInfo).setRedirectURL(redirectURL).build();
	}
	
	@Override
	public String toString()
	{
		return "WorkflowFinalizationConfiguration [success=" + success + ", autoRedirect=" + autoRedirect
				+ ", pageTitle=" + pageTitle + ", logoURL=" + logoURL + ", mainInformation="
				+ mainInformation + ", extraInformation=" + extraInformation + ", redirectURL="
				+ redirectURL + ", redirectButtonText=" + redirectButtonText + ", redirectAfterTime=" + redirectAfterTime
				+ ", isAutoLoginAfterSignUp=" + isAutoLoginAfterSignUp + "]";
	}

	public static class Builder
	{
		private boolean success;
		private boolean autoRedirect;
		private String pageTitle;
		private String logoURL;
		private String mainInformation;
		private String extraInformation;
		private String redirectURL;
		private String redirectButtonText;
		private int redirectAfter;
		
		public Builder setSuccess(boolean success)
		{
			this.success = success;
			return this;
		}
		public Builder setAutoRedirect(boolean autoRedirect)
		{
			this.autoRedirect = autoRedirect;
			return this;
		}
		public Builder setPageTitle(String pageTitle)
		{
			this.pageTitle = pageTitle;
			return this;
		}
		public Builder setLogoURL(String logoURL)
		{
			this.logoURL = logoURL;
			return this;
		}
		public Builder setMainInformation(String mainInformation)
		{
			this.mainInformation = mainInformation;
			return this;
		}
		public Builder setExtraInformation(String extraInformation)
		{
			this.extraInformation = extraInformation;
			return this;
		}
		public Builder setRedirectURL(String redirectURL)
		{
			this.redirectURL = redirectURL;
			return this;
		}
		public Builder setRedirectButtonText(String redirectButtonText)
		{
			this.redirectButtonText = redirectButtonText;
			return this;
		}
		
		public Builder setRedirectAfter(int redirectAfter)
		{
			this.redirectAfter = redirectAfter;
			return this;
		}
		
		
		public WorkflowFinalizationConfiguration build()
		{
			return new WorkflowFinalizationConfiguration(success, autoRedirect, pageTitle, 
					logoURL, mainInformation, extraInformation, redirectURL, redirectButtonText, redirectAfter); 
		}
	}
}
