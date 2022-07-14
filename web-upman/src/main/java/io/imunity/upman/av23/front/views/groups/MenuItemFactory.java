/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.groups;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.upman.av23.front.components.FormLayoutLabel;
import io.imunity.upman.av23.front.components.LocaleTextField;
import io.imunity.upman.av23.front.components.LocaleTextFieldDetails;
import io.imunity.upman.av23.front.components.MenuButton;
import io.imunity.upman.av23.front.model.Group;
import io.imunity.upman.av23.front.model.ProjectGroup;
import pl.edu.icm.unity.MessageSource;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition.ASIDE;
import static com.vaadin.flow.component.icon.VaadinIcon.*;

class MenuItemFactory
{
	private final GroupService groupService;
	private final MessageSource msg;
	private final Div content;
	private final Runnable viewReloader;

	MenuItemFactory(MessageSource msg, GroupService groupService, Div content, Runnable reloader)
	{
		this.groupService = groupService;
		this.msg = msg;
		this.content = content;
		this.viewReloader = reloader;
	}

	MenuItem createMakePrivateItem(ProjectGroup projectGroup, Group group)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupsComponent.makePrivateAction"), LOCK);
		return new MenuItem(menuButton, event -> groupService.setGroupAccessMode(projectGroup, group, false));
	}

	MenuItem createMakePublicItem(ProjectGroup projectGroup, Group group)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupsComponent.makePublicAction"), UNLOCK);
		return new MenuItem(menuButton, event -> groupService.setGroupAccessMode(projectGroup, group, true));
	}

	MenuItem createAddGroupItem(ProjectGroup projectGroup, Group group, boolean subGroupAvailable)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupsComponent.addGroupAction"), PLUS_CIRCLE_O);
		return new MenuItem(menuButton, event -> createAddGroupDialog(projectGroup, group, subGroupAvailable).open());
	}

	MenuItem createDeleteGroupItem(ProjectGroup projectGroup, Group group)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupsComponent.deleteGroupAction"), BAN);
		return new MenuItem(menuButton, event -> createConfirmDialog(msg.getMessage("RemoveGroupDialog.confirmDelete", group.displayedName), () -> groupService.deleteGroup(projectGroup, group)).open());
	}

	MenuItem createDeleteSubGroupItem(ProjectGroup projectGroup, Group group)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupsComponent.deleteSubprojectGroupAction", group.displayedName), BAN);
		return new MenuItem(menuButton, event -> createConfirmDialog(msg.getMessage("RemoveGroupDialog.confirmSubprojectDelete", group.displayedName), () -> groupService.deleteSubProjectGroup(projectGroup, group)).open());
	}

	MenuItem createRenameGroupItem(ProjectGroup projectGroup, Group group)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupsComponent.renameGroupAction"), PENCIL);
		return new MenuItem(menuButton, event -> createRenameDialog(projectGroup, group).open());
	}

	MenuItem createDelegateGroupItem(ProjectGroup projectGroup, Group group)
	{
		MenuButton menuButton = new MenuButton(msg.getMessage("GroupsComponent.delegateGroupAction"), WORKPLACE);
		return new MenuItem(menuButton, event -> createDelegateDialog(projectGroup, group).open());
	}

	private Dialog createDelegateDialog(ProjectGroup projectGroup, Group group)
	{
		Dialog dialog = createBaseDialog(msg.getMessage("SubprojectDialog.caption"));

		SubProjectConfigurationLayout subProjectConfigurationLayout = new SubProjectConfigurationLayout(msg, content, group);
		dialog.add(subProjectConfigurationLayout);

		Button saveButton = new Button(msg.getMessage("OK"));
		saveButton.addClickListener(event ->
		{
			groupService.setGroupDelegationConfiguration(projectGroup, group, subProjectConfigurationLayout.enableDelegation.getValue(), subProjectConfigurationLayout.enableSubprojects.getValue(), subProjectConfigurationLayout.logoUrl.getValue());
			viewReloader.run();
			dialog.close();
		});

		dialog.getFooter().add(saveButton);

		return dialog;
	}

	private Dialog createAddGroupDialog(ProjectGroup projectGroup, Group group, boolean subGroupAvailable)
	{
		Dialog dialog = createBaseDialog(msg.getMessage("AddGroupDialog.caption"));

		FormLayout dialogLayout = new FormLayout();
		dialogLayout.setWidth("45em");
		LocaleTextFieldDetails localeTextFieldDetails = new LocaleTextFieldDetails(msg, msg.getMessage("AddGroupDialog.info", group.displayedName));

		Checkbox isPublic = new Checkbox(msg.getMessage("AddGroupDialog.public"));
		isPublic.setValue(group.isPublic);
		isPublic.setEnabled(group.isPublic);

		dialogLayout.addFormItem(localeTextFieldDetails, new FormLayoutLabel(msg.getMessage("GroupsComponent.newGroupName")));
		dialogLayout.addFormItem(isPublic, "");
		dialogLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("1em", 1, ASIDE));

		SubProjectConfigurationLayout subProjectConfigurationLayout = new SubProjectConfigurationLayout(msg, content);

		if(!subGroupAvailable)
			subProjectConfigurationLayout.setVisible(false);

		VerticalLayout verticalLayout = new VerticalLayout(dialogLayout, subProjectConfigurationLayout);
		verticalLayout.getStyle().set("gap", "unset");
		dialog.add(verticalLayout);

		Button saveButton = new Button(msg.getMessage("OK"));
		saveButton.addClickListener(event ->
		{
			Map<Locale, String> localeToTxt = localeTextFieldDetails.fields.stream()
					.collect(Collectors.toMap(field -> field.locale, TextField::getValue));
			groupService.addGroup(projectGroup, group, localeToTxt, isPublic.getValue());
			if(subProjectConfigurationLayout.enableDelegation.getValue())
				groupService.setGroupDelegationConfiguration(projectGroup, group,
						subProjectConfigurationLayout.enableDelegation.getValue(),
						subProjectConfigurationLayout.enableSubprojects.getValue(),
						subProjectConfigurationLayout.logoUrl.getValue()
				);
			viewReloader.run();
			dialog.close();
		});

		dialog.getFooter().add(saveButton);

		return dialog;
	}

	private Dialog createConfirmDialog(String txt, Runnable runnable)
	{
		Dialog dialog = createBaseDialog(msg.getMessage("Confirmation"));

		Label label = new Label(txt);

		HorizontalLayout dialogLayout = new HorizontalLayout();
		dialogLayout.add(label);
		dialogLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		dialog.add(dialogLayout);

		Button saveButton = new Button(msg.getMessage("OK"));
		saveButton.addClickListener(event ->
		{
			runnable.run();
			viewReloader.run();
			dialog.close();
		});
		dialog.getFooter().add(saveButton);

		return dialog;
	}

	private Dialog createRenameDialog(ProjectGroup projectGroup, Group group)
	{
		Dialog dialog = createBaseDialog(msg.getMessage("GroupsComponent.renameGroupAction"));

		LocaleTextFieldDetails details = new LocaleTextFieldDetails(msg, "");

		HorizontalLayout dialogLayout = new HorizontalLayout();
		dialogLayout.add(details);
		dialogLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		dialog.add(dialogLayout);

		Button saveButton = createRenameButton(projectGroup, group, dialog, details.fields);
		dialog.getFooter().add(saveButton);

		return dialog;
	}

	private Dialog createBaseDialog(String header)
	{
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle(header);
		Button cancelButton = new Button(msg.getMessage("Cancel"), e -> dialog.close());
		dialog.getFooter().add(cancelButton);
		content.add(dialog);
		return dialog;
	}

	private Button createRenameButton(ProjectGroup projectGroup, Group group, Dialog dialog, List<LocaleTextField> fields)
	{
		Button button = new Button(msg.getMessage("OK"));
		button.addClickListener(event ->
		{
			Map<Locale, String> collect = fields.stream().collect(Collectors.toMap(x -> x.locale, TextField::getValue));
			groupService.updateGroupName(projectGroup, group, collect);
			viewReloader.run();
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
