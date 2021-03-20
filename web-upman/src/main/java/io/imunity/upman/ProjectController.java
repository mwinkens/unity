/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.server.Resource;

import io.imunity.upman.common.ServerFaultException;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Controller for project management
 * 
 * @author P.Piernik
 *
 */
@Component
public class ProjectController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_UPMAN, ProjectController.class);

	private MessageSource msg;
	private DelegatedGroupManagement delGroupMan;
	private ImageAccessService imageAccessService;

	@Autowired
	public ProjectController(MessageSource msg, DelegatedGroupManagement delGroupMan, ImageAccessService imageAccessService)
	{
		this.msg = msg;
		this.delGroupMan = delGroupMan;
		this.imageAccessService = imageAccessService;
	}

	Map<String, String> getProjectForUser(long entityId) throws ControllerException
	{

		List<DelegatedGroup> projects;
		try
		{
			projects = delGroupMan.getProjectsForEntity(entityId);
		} catch (Exception e)
		{
			log.debug("Can not get projects for entity " + entityId, e);
			throw new ServerFaultException(msg);
		}

		if (projects.isEmpty())
			throw new ControllerException(
					msg.getMessage("ProjectController.noProjectAvailable"),
					null);

		return projects.stream().collect(Collectors.toMap(p -> p.path, p -> p.displayedName));
	}

	public Resource getProjectLogoOrNull(String projectPath)
	{
		Resource logo = Images.logoSmall.getResource();
		DelegatedGroup group;
		try
		{
			group = delGroupMan.getContents(projectPath, projectPath).group;
		} catch (Exception e)
		{
			return logo;
		}
		GroupDelegationConfiguration config = group.delegationConfiguration;
		return imageAccessService.getConfiguredImageResourceFromNullableUri(config.logoUrl).orElse(null);
	}
	
	public DelegatedGroup getProjectGroup(String projectPath) throws ControllerException
	{
		try
		{
			return delGroupMan.getContents(projectPath, projectPath).group;
		}
		catch (Exception e)
		
		{
			log.debug("Can not get project group " + projectPath, e);
			throw new ServerFaultException(msg);
		}
	}
	
	public GroupAuthorizationRole getProjectRole(String projectPath) throws ControllerException
	{
		try
		{

			return delGroupMan.getGroupAuthorizationRole(projectPath,
					InvocationContext.getCurrent().getLoginSession().getEntityId());

		} catch (Exception e)
		{
			log.debug("Can not get project authorization role " + projectPath, e);
			throw new ServerFaultException(msg);
		}
	}

}
