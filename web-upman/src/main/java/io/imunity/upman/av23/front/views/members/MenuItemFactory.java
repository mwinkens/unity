/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.members;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import io.imunity.upman.av23.front.components.BaseDialog;
import io.imunity.upman.av23.front.components.MenuButton;
import io.imunity.upman.av23.front.model.Group;
import io.imunity.upman.av23.front.model.GroupTreeNode;
import io.imunity.upman.av23.front.model.ProjectGroup;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static com.vaadin.flow.component.icon.VaadinIcon.*;
import static java.util.stream.Collectors.toList;

class MenuItemFactory
{
	private final GroupMembersService groupMembersController;
	private final MessageSource msg;
	private final Div content;
	private final Runnable viewReloader;

	MenuItemFactory(MessageSource msg, GroupMembersService groupMembersController, Div content, Runnable reloader)
	{
		this.groupMembersController = groupMembersController;
		this.msg = msg;
		this.content = content;
		this.viewReloader = reloader;
	}

	MenuItem createRemoveFromProjectItem(Supplier<ProjectGroup> projectGetter, Supplier<Group> groupGetter,
	                                     Supplier<Set<MemberModel>> modelsGetter)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupMembersComponent.removeFromProjectAction"), BAN);
		return new MenuItem(menuButton, event -> removeFromGroup(projectGetter.get(), groupGetter.get(), modelsGetter.get()));
	}

	MenuItem createRemoveFromGroupItem(Supplier<ProjectGroup> projectGetter, Supplier<Group> groupGetter,
	                                   Supplier<Set<MemberModel>> modelsGetter)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupMembersComponent.removeFromGroupAction"), FILE_REMOVE);
		return new MenuItem(menuButton, event -> removeFromGroup(projectGetter.get(), groupGetter.get(), modelsGetter.get()));
	}

	MenuItem createAddToGroupItem(Supplier<ProjectGroup> projectGetter, Supplier<List<GroupTreeNode>> groupGetter, Supplier<Set<MemberModel>> modelsGetter)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupMembersComponent.addToGroupAction"), PLUS_CIRCLE_O);
		return new MenuItem(menuButton, event -> createAddToGroupDialog(projectGetter.get(), modelsGetter.get(), groupGetter.get()).open());
	}

	MenuItem createSetProjectRoleItem(Supplier<ProjectGroup> projectGetter, Supplier<Group> groupGetter, Supplier<Set<MemberModel>> modelsGetter)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupMembersComponent.setProjectRoleAction"), STAR_O);
		return new MenuItem(menuButton, event -> createSetProjectRoleDialog(projectGetter.get(), groupGetter.get(), modelsGetter.get()).open());
	}

	MenuItem createSetSubProjectRoleItem(Supplier<ProjectGroup> projectGetter, Supplier<Group> groupGetter, Supplier<Set<MemberModel>> modelsGetter)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupMembersComponent.setSubProjectRoleAction"), STAR_O);
		return new MenuItem(menuButton, event -> createSetSubProjectRoleDialog(projectGetter.get(), groupGetter.get(), modelsGetter.get()).open());
	}

	private void removeFromGroup(ProjectGroup projectGroup, Group group, Set<MemberModel> models)
	{
		if(isCurrentUserSelected(models))
		{
			String message = msg.getMessage("GroupMembersComponent.confirmSelfRemoveFromProject", projectGroup.displayedName);
			createSelfRemoveDialog(
				message, () ->
					{
						groupMembersController.removeFromGroup(projectGroup, group, models);
						viewReloader.run();
					}
			).open();
		}
		else
		{
			groupMembersController.removeFromGroup(projectGroup, group, models);
			viewReloader.run();
		}
	}

	private boolean isCurrentUserSelected(Set<MemberModel> models)
	{
		long entityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
		return models.stream().map(member -> member.entityId).anyMatch(memberId -> memberId.equals(entityId));
	}


	private Dialog createSelfRemoveDialog(String txt, Runnable job)
	{
		Dialog dialog = createBaseDialog(msg.getMessage("Confirmation"));
		dialog.add(new VerticalLayout(new Label(txt)));

		Button saveButton = new Button(msg.getMessage("OK"), e ->
		{
			job.run();
			dialog.close();
		});
		dialog.getFooter().add(saveButton);

		return dialog;
	}

	private Dialog createAddToGroupDialog(ProjectGroup projectGroup, Set<MemberModel> members, List<GroupTreeNode> groups)
	{
		Dialog dialog = createBaseDialog(msg.getMessage("AddToGroupDialog.caption"));

		ComboBox<GroupTreeNode> groupComboBox = new GroupComboBox();
		groupComboBox.setLabel(msg.getMessage("AddToGroupDialog.info"));
		groupComboBox.setItems(groups);
		if(groups.iterator().hasNext())
			groupComboBox.setValue(groups.iterator().next());
		groupComboBox.getStyle().set("width", "24em");

		HorizontalLayout dialogLayout = new HorizontalLayout();
		dialogLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
		dialogLayout.add(new Label(msg.getMessage("AddToGroupDialog.selectGroup")), groupComboBox);
		dialog.add(dialogLayout);

		Button saveButton = createAddToGroupButton(projectGroup, dialog, groupComboBox, members);
		dialog.getFooter().add(saveButton);

		return dialog;
	}

	private Dialog createSetProjectRoleDialog(ProjectGroup projectGroup, Group group, Set<MemberModel> items)
	{
		Dialog dialog = createBaseDialog(msg.getMessage("RoleSelectionDialog.projectCaption"));

		RadioButtonGroup<GroupAuthorizationRole> radioGroup = createRoleRadioButtonGroup();
		Label label = new Label(msg.getMessage("RoleSelectionDialog.projectRole"));

		HorizontalLayout dialogLayout = new HorizontalLayout();
		dialogLayout.add(label, radioGroup);
		dialogLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		dialog.add(dialogLayout);

		Button saveButton = createSetProjectRoleButton(projectGroup, group, dialog, radioGroup, items);
		dialog.getFooter().add(saveButton);

		return dialog;
	}

	private RadioButtonGroup<GroupAuthorizationRole> createRoleRadioButtonGroup()
	{
		RadioButtonGroup<GroupAuthorizationRole> radioGroup = new RadioButtonGroup<>();
		radioGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
		radioGroup.setItems(GroupAuthorizationRole.values());
		radioGroup.setItemLabelGenerator(role -> msg.getMessage("Role." + role.toString().toLowerCase()));
		return radioGroup;
	}

	private Dialog createSetSubProjectRoleDialog(ProjectGroup projectGroup, Group group, Set<MemberModel> items)
	{
		Dialog dialog = createBaseDialog(msg.getMessage("RoleSelectionDialog.subprojectCaption"));

		RadioButtonGroup<GroupAuthorizationRole> radioGroup = createRoleRadioButtonGroup();
		Label label = new Label(msg.getMessage("RoleSelectionDialog.subprojectRole"));

		HorizontalLayout dialogLayout = new HorizontalLayout();
		dialogLayout.add(label, radioGroup);
		dialogLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		dialog.add(dialogLayout);

		Button saveButton = createSetSubProjectRoleButton(projectGroup, group, dialog, radioGroup, items);
		dialog.getFooter().add(saveButton);

		return dialog;
	}

	private Dialog createBaseDialog(String header)
	{
		return new BaseDialog(header, msg.getMessage("Cancel"), content);
	}

	private Button createSetSubProjectRoleButton(ProjectGroup projectGroup, Group group, Dialog dialog, RadioButtonGroup<GroupAuthorizationRole> radioGroup, Set<MemberModel> items)
	{
		Button button = new Button(msg.getMessage("OK"));
		button.addClickListener(event ->
		{
			groupMembersController.updateRole(projectGroup, group, radioGroup.getValue(), items);
			viewReloader.run();
			dialog.close();
		});
		return button;
	}

	private Button createSetProjectRoleButton(ProjectGroup projectGroup, Group group, Dialog dialog, RadioButtonGroup<GroupAuthorizationRole> radioGroup, Set<MemberModel> items)
	{
		Button button = new Button(msg.getMessage("OK"));
		button.addClickListener(event ->
		{
			GroupAuthorizationRole role = radioGroup.getValue();
			if(role.equals(GroupAuthorizationRole.regular) && isCurrentUserSelected(items))
			{
				dialog.close();
				createSelfRemoveDialog(
						msg.getMessage("GroupMembersComponent.confirmSelfRevokeManagerPrivileges", projectGroup.displayedName),
						() ->
						{
							groupMembersController.updateRole(projectGroup, group, role, items);
							viewReloader.run();
						}
				);
				return;
			}
			groupMembersController.updateRole(projectGroup, group, role, items);
			viewReloader.run();
			dialog.close();
		});
		return button;
	}

	private Button createAddToGroupButton(ProjectGroup projectGroup, Dialog dialog, ComboBox<GroupTreeNode> comboBox, Set<MemberModel> members)
	{
		Button button = new Button(msg.getMessage("OK"));
		button.addClickListener(event ->
		{
			GroupTreeNode value = comboBox.getValue();
			List<GroupTreeNode> parents = value.getAllNodes();
			parents.add(value);

			groupMembersController.addToGroup(projectGroup, parents.stream().map(node -> node.group).collect(toList()), members);
			dialog.close();
		});
		return button;
	}

	static class MenuItem
	{
		Component component;
		ComponentEventListener<ClickEvent<com.vaadin.flow.component.contextmenu.MenuItem>> clickListener;

		private MenuItem(Component component, ComponentEventListener<ClickEvent<com.vaadin.flow.component.contextmenu.MenuItem>> clickListener)
		{
			this.component = component;
			this.clickListener = clickListener;
		}
	}
}
