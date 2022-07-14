/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.groups;

import io.imunity.upman.av23.front.components.NotificationPresenter;
import io.imunity.upman.av23.front.model.Group;
import io.imunity.upman.av23.front.model.ProjectGroup;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.SubprojectGroupDelegationConfiguration;
import pl.edu.icm.unity.types.I18nString;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
class GroupService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_UPMAN, GroupService.class);

	private final DelegatedGroupManagement delGroupMan;
	private final MessageSource msg;

	public GroupService(MessageSource msg, DelegatedGroupManagement delGroupMan)
	{
		this.msg = msg;
		this.delGroupMan = delGroupMan;
	}

	public void addGroup(ProjectGroup projectGroup, Group group, Map<Locale, String> names, boolean isPublic)
	{
		I18nString i18nString = cretaei18nString(names);
		try
		{
			delGroupMan.addGroup(projectGroup.path, group.path, i18nString, isPublic);
		} catch (Exception e)
		{
			log.warn("Can not add group " + projectGroup.path, e);
			NotificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
		}
	}

	public void deleteGroup(ProjectGroup projectGroup, Group group)
	{
		try
		{
			delGroupMan.removeGroup(projectGroup.path, group.path);
		} catch (Exception e)
		{
			log.warn("Can not remove group " + group, e);
			NotificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
		}
	}
	
	public void deleteSubProjectGroup(ProjectGroup projectGroup, Group group)
	{
		try
		{
			delGroupMan.removeProject(projectGroup.path, group.path);
		} catch (Exception e)
		{
			log.warn("Can not remove sub-project group " + group.path, e);
			NotificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
		}
	}

	public void setGroupAccessMode(ProjectGroup projectGroup, Group group, boolean isPublic)
	{
		try
		{
			delGroupMan.setGroupAccessMode(projectGroup.path, group.path, isPublic);

		} catch (Exception e)
		{
			log.warn("Can not set group access mode for " + group.path, e);

			if (!projectGroup.path.equals(group.path))
			{
				NotificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
			} else
			{
				NotificationPresenter.showError(msg.getMessage("GroupsController.projectGroupAccessModeChangeError"), msg.getMessage("GroupsController.projectGroupAccessModeChangeErrorDetails"));
			}
		}
	}

	public void updateGroupName(ProjectGroup projectGroup, Group group, Map<Locale, String> names)
	{
		I18nString i18nString = cretaei18nString(names);
		try
		{

			delGroupMan.setGroupDisplayedName(projectGroup.path, group.path, i18nString);

		} catch (Exception e)
		{
			log.warn("Can not rename group " + group.path, e);
			NotificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
		}
	}

	private I18nString cretaei18nString(Map<Locale, String> names)
	{
		I18nString i18nString = new I18nString();
		Map<String, String> values = names.entrySet().stream()
				.filter(entry -> !entry.getValue().isBlank())
				.collect(Collectors.toMap(entry -> entry.getKey().getLanguage(), Map.Entry::getValue));
		i18nString.addAllValues(values);
		return i18nString;
	}

	public void setGroupDelegationConfiguration(ProjectGroup projectGroup, Group group, boolean enabled, boolean enableSubProjects, String logUrl)
	{
		SubprojectGroupDelegationConfiguration config = new SubprojectGroupDelegationConfiguration(enabled, enableSubProjects, logUrl);
		try
		{
			delGroupMan.setGroupDelegationConfiguration(projectGroup.path, group.path, config);

		} catch (Exception e)
		{
			log.warn("Can not set group delegation configuration in " + group.path, e);
			NotificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
		}

	}
}
