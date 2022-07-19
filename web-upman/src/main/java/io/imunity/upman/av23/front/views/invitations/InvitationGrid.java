/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.invitations;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.upman.av23.front.components.TooltipPiner;
import pl.edu.icm.unity.MessageSource;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

@CssImport("./styles/components/invitations-grid.css")
class InvitationGrid extends Grid<InvitationModel>
{
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	public InvitationGrid(MessageSource msg, Function<InvitationModel, Component> actionMenuGetter, HtmlContainer container)
	{
		addColumn(model -> model.email)
				.setHeader(msg.getMessage("Invitation.email"))
				.setAutoWidth(true)
				.setSortable(true);
		addComponentColumn(model -> createGroupsLabel(model, container))
				.setHeader(msg.getMessage("Invitation.groups"))
				.setFlexGrow(5)
				.setSortable(true);
		addColumn(model -> formatter.format(model.requestedTime))
				.setHeader(msg.getMessage("Invitation.lastSent"))
				.setAutoWidth(true)
				.setSortable(true);
		addColumn(model -> formatter.format(model.expirationTime))
				.setHeader(msg.getMessage("Invitation.expiration"))
				.setAutoWidth(true)
				.setSortable(true);
		addComponentColumn(model -> new Anchor(model.link, VaadinIcon.EXTERNAL_LINK.create()))
				.setAutoWidth(true)
				.setTextAlign(ColumnTextAlign.END)
				.setHeader(msg.getMessage("Invitation.link"));
		addComponentColumn(actionMenuGetter::apply)
				.setHeader(msg.getMessage("Invitation.action"))
				.setAutoWidth(true)
				.setTextAlign(ColumnTextAlign.END);
		setClassNameGenerator(model -> model.expirationTime.isBefore(Instant.now()) ? "light-red-row" : "usual-row");

		addThemeName("no-border");
		setSelectionMode(Grid.SelectionMode.MULTI);
	}

	private Label createGroupsLabel(InvitationModel model, HtmlContainer container)
	{
		String groups = String.join(", ", model.groupsDisplayedNames);
		Label label = new Label(groups);
		TooltipPiner.pinTooltip(groups, label, container);
		return label;
	}
}
