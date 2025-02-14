/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;

import java.util.List;
import java.util.Map;

/**
 * Internal engine API for delegated groups management
 * @author P.Piernik
 *
 */
public interface DelegatedGroupManagement
{
	/**
	 * Adds group
	 * 
	 * @param projectPath project group path
	 * @param parentPath parent group path
	 * @param groupName new group name
	 * @param isPublic group access mode
	 * @return 
	 * @throws EngineException
	 */
	String addGroup(String projectPath, String parentPath, I18nString groupName, boolean isPublic)
			throws EngineException;

	/**
	 * Removes group
	 * 
	 * @param projectPath project group path
	 * @param path removed group path
	 * @throws EngineException
	 */
	void removeGroup(String projectPath, String path) throws EngineException;
	
	/**
	 * Removes subproject
	 * 
	 * @param projectPath project group path
	 * @param subProjectPath removed subproject group path
	 * @throws EngineException
	 */
	void removeProject(String projectPath, String subProjectPath) throws EngineException;

	
	/**
	 * Allows to retrieve group's contents and metadata. 
	 * 
	 * @param subgroupPath group to be queried.
	 * @return
	 * @throws EngineException
	 */
	DelegatedGroupContents getContents(String projectPath, String subgroupPath) throws EngineException;

	/**
	 * Sets group display name
	 * 
	 * @param projectPath project group path
	 * @param path renamed group path
	 * @param newName 
	 * @throws EngineException
	 */
	void setGroupDisplayedName(String projectPath, String path, I18nString newName)
			throws EngineException;

	/**
	 * Updates group access mode
	 * 
	 * @param projectPath project group path
	 * @param path updated group path
	 * @param isPublic indicates is group public or private mode
	 * @throws EngineException
	 */
	void setGroupAccessMode(String projectPath, String path, boolean isPublic)
			throws EngineException;

	/**
	 * Gets group with all child (recursive) groups as map.
	 *
	 * @param projectPath is a full path to the project's underlying group
	 * @param subgroupPath is a full path to a subgroup of the project of which groups will be received
	 * @return keys of the returned map include the selected group and all its children. Values are 
	 * objects with group's metadata and subgroups
	 * @throws EngineException
	 */

	Map<String, DelegatedGroupContents> getGroupAndSubgroups(String projectPath, String subgroupPath)
			throws EngineException;

	
	/**
	 * Gets attribute displayed name
	 * 
	 * @param projectPath project group path
	 * @param attributeName
	 * @return attribute display name
	 * @throws EngineException
	 */
	String getAttributeDisplayedName(String projectPath, String attributeName)
			throws EngineException;

	/**
	 * Update value of group authorization role attribute
	 *
	 * @param projectPath is a full path to the project's underlying group
	 * @param subgroupPath is a full path to a subgroup of the project of which authorization role will be set
	 * @param entityId attribute owner
	 * @param role value to set
	 * @throws EngineException
	 */
	void setGroupAuthorizationRole(String projectPath, String subgroupPath, long entityId, GroupAuthorizationRole role)
			throws EngineException;

	
	/**
	 * Update value of group authorization role attribute 
	 * 
	 * @param projectPath project group path
	 * @param entityId attribute owner
	 * @param role value to set
	 * @throws EngineException
	 */
	GroupAuthorizationRole getGroupAuthorizationRole(String projectPath, long entityId)
			throws EngineException;

	/**
	 * Sets group delegation configuration 
	 * @param projectPath is a full path to the project's underlying group
	 * @param subgroupPath is a full path to a subgroup of the project of which delegation configuration will be added
	 * @param subprojectGroupDelegationConfiguration group delegation configuration to set
	 * @throws EngineException
	 */
	void setGroupDelegationConfiguration(String projectPath, String subgroupPath,
			SubprojectGroupDelegationConfiguration subprojectGroupDelegationConfiguration)
			throws EngineException;
	
	/**
	 * Gets projects for entity
	 * 
	 * @param entityId project manager
	 * @return All project group of entity
	 * @throws EngineException
	 */
	List<DelegatedGroup> getProjectsForEntity(long entityId) throws EngineException;

	/**
	 * Adds a new member to the group
	 *
	 * @param projectPath is a full path to the project's underlying group
	 * @param subgroupPath is a full path to a subgroup of the project of which member will be added
	 * @param entityId entity id to add
	 * @throws EngineException
	 */
	void addMemberToGroup(String projectPath, String subgroupPath, long entityId)
			throws EngineException;

	/**
	 * Removes from the group and all subgroups if the user is in any. 
	 * Entity can not be removed from the group == '/' 
	 *
	 * @param projectPath is a full path to the project's underlying group
	 * @param subgroupPath is a full path to a subgroup of the project of which member will be removed
	 * @param entityId entity id to remove
	 * @throws EngineException
	 */
	void removeMemberFromGroup(String projectPath, String subgroupPath, long entityId)
			throws EngineException;

	/**
	 * Gets delegated group members
	 * @param projectPath is a full path to the project's underlying group
	 * @param subgroupPath is a full path to a subgroup of the project of which members will be returned
	 * @return
	 * @throws EngineException
	 */
	List<DelegatedGroupMember> getDelegatedGroupMembers(String projectPath, String subgroupPath)
			throws EngineException;

	
}
