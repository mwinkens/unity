/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.server.SynchronizedRequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.communication.ServletBootstrapHandler;
import com.vaadin.shared.Version;

import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.FreemarkerUtils;

/**
 * Handler responsible for returning a bootstrap web page - on first page load. Used
 * instead of the default Vaadin's {@link ServletBootstrapHandler}.
 * <p>
 * The page is generated by Freemarker from a configured template.
 * 
 * @author K. Benedyczak
 */
public class UnityBootstrapHandler extends SynchronizedRequestHandler
{
	private final Configuration cfg;
	private final String mainTemplate;
	private final UnityMessageSource msg;
	private String theme;
	private boolean debug;
	private long heartbeat;
	private String uiPath;
	
	public UnityBootstrapHandler(String webContentsDirectory, String mainTemplate, UnityMessageSource msg, String theme,
			boolean debug, long heartbeat, String uiPath)
	{
		this.mainTemplate = mainTemplate;
		this.msg = msg;
		this.theme = theme;
		this.debug = debug;
		this.heartbeat = heartbeat;
		this.uiPath = uiPath;
		cfg = new Configuration(Configuration.VERSION_2_3_21);
		
		cfg.setTemplateLoader(FreemarkerUtils.getTemplateLoader(webContentsDirectory, 
				FreemarkerUtils.TEMPLATES_ROOT, getClass()));
		BeansWrapperBuilder builder = new BeansWrapperBuilder(Configuration.VERSION_2_3_21);
		cfg.setObjectWrapper(builder.build());
	}

	@Override
	public boolean synchronizedHandleRequest(VaadinSession session, VaadinRequest request,
			VaadinResponse response) throws IOException
	{
		response.setContentType("text/html; charset=utf-8");
	        response.setHeader("Cache-Control", "no-cache");
	        response.setHeader("Pragma", "no-cache");
	        response.setDateHeader("Expires", 0);
	        FreemarkerUtils.processTemplate(cfg, mainTemplate, createContext(), response.getWriter());
		return true;
	}
	
	private Map<String, String> createContext()
	{
		Map<String, String> data = new HashMap<String, String>();
		data.put("theme", theme);
		data.put("uiPath", uiPath);
		data.put("vaadinVersion", Version.getFullVersion());
		data.put("debug", String.valueOf(debug));
		data.put("heartbeat", String.valueOf(heartbeat));
		data.put("comErrMsgCaption", msg.getMessage("UIWrappingServlet.comErrMsgCaption"));
		data.put("comErrMsg", msg.getMessage("UIWrappingServlet.comErrMsg"));
		data.put("authErrMsgCaption", msg.getMessage("UIWrappingServlet.authErrMsgCaption"));
		data.put("authErrMsg", msg.getMessage("UIWrappingServlet.authErrMsg"));
		data.put("sessExpMsgCaption", msg.getMessage("UIWrappingServlet.sessExpMsgCaption"));
		data.put("sessExpMsg", msg.getMessage("UIWrappingServlet.sessExpMsg"));
		return data;
	}
}
