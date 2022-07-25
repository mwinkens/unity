/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.invitations;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.imunity.upman.av23.front.model.GroupTreeNode;
import org.vaadin.gatanaso.MultiselectComboBox;

import java.util.HashSet;
import java.util.Set;

class GroupMultiComboBox extends MultiselectComboBox<GroupTreeNode>
{
	GroupMultiComboBox()
	{
		setRenderer(new ComponentRenderer<>(this::renderGroupWithIndent));
		addValueChangeListener(this::blockNullValue);
		setItemLabelGenerator(event -> event.group.displayedName);

		addValueChangeListener(event ->
		{
			if(!event.isFromClient())
				return;

			Set<GroupTreeNode> selectedGroups = new HashSet<>(event.getValue());
			Set<GroupTreeNode> lastSelectedGroup = new HashSet<>(event.getOldValue());

			addAllGroupsAncestorIfNewGroupAdded(selectedGroups, lastSelectedGroup);
			removeAllOffspringsIfParentWasRemoved(selectedGroups, lastSelectedGroup);

			setValue(selectedGroups);
		});
	}

	private void removeAllOffspringsIfParentWasRemoved(Set<GroupTreeNode> newSet, Set<GroupTreeNode> oldSet)
	{
		HashSet<GroupTreeNode> nodes = new HashSet<>(oldSet);
		nodes.removeAll(newSet);
		nodes.stream()
				.map(GroupTreeNode::getNodeWithAllOffspring)
				.forEach(newSet::removeAll);
	}

	private void addAllGroupsAncestorIfNewGroupAdded(Set<GroupTreeNode> newSet, Set<GroupTreeNode> oldSet)
	{
		HashSet<GroupTreeNode> nodes = new HashSet<>(newSet);
		nodes.removeAll(oldSet);
		nodes.stream()
				.map(GroupTreeNode::getAllAncestors)
				.forEach(newSet::addAll);
	}

	private void blockNullValue(ComponentValueChangeEvent<MultiselectComboBox<GroupTreeNode>, Set<GroupTreeNode>> event)
	{
		if(event.getValue() == null && event.isFromClient())
			setValue(event.getOldValue());
	}

	private Div renderGroupWithIndent(GroupTreeNode group)
	{
		Div div = new Div(new Text(group.getDisplayedName()));
		div.getStyle().set("text-indent", group.getLevel() + "em");
		return div;
	}
}
