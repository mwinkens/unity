/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;

import io.imunity.scim.scheme.DefaultSchemaProvider;
import io.imunity.scim.scheme.SchemaResourceDeserialaizer;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.webui.common.FileUploder;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase.EditorTab;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent.ServiceEditorTab;

class SCIMServiceEditorSchemaTab extends CustomComponent implements EditorTab
{
	private final MessageSource msg;
	private final SubViewSwitcher subViewSwitcher;
	private final UnityServerConfiguration unityServerConfiguration;

	SCIMServiceEditorSchemaTab(MessageSource msg, UnityServerConfiguration unityServerConfiguration,
			SubViewSwitcher subViewSwitcher)
	{
		this.msg = msg;
		this.unityServerConfiguration = unityServerConfiguration;
		this.subViewSwitcher = subViewSwitcher;

	}

	void initUI(Binder<SCIMServiceConfigurationBean> configBinder)
	{
		setIcon(Images.attributes.getResource());
		setCaption(msg.getMessage("SCIMServiceEditorSchemaTab.schemas"));
		VerticalLayout mainWrapper = new VerticalLayout();
		mainWrapper.setMargin(false);

		SchemasComponent schemas = new SchemasComponent();
		configBinder.forField(schemas).bind("schemas");
		mainWrapper.addComponent(schemas);

		setCompositionRoot(mainWrapper);
	}

	@Override
	public String getType()
	{
		return ServiceEditorTab.OTHER.toString();
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	private class SchemasComponent extends CustomField<List<SchemaWithMappingBean>>
	{
		private GridWithActionColumn<SchemaWithMappingBean> schemasGrid;
		private VerticalLayout main;
		private FileUploder uploader;

		public SchemasComponent()
		{
			initUI();
		}

		private void initUI()
		{
			main = new VerticalLayout();
			main.setMargin(new MarginInfo(true, false));

			HorizontalLayout buttons = new HorizontalLayout();
			main.addComponent(buttons);
			main.setComponentAlignment(buttons, Alignment.MIDDLE_RIGHT);

			Button add = new Button(msg.getMessage("create"));
			add.addClickListener(e ->
			{
				setComponentError(null);
				gotoNew();
			});
			add.setIcon(Images.add.getResource());
			add.setStyleName(Styles.buttonAction.toString());

			Upload upload = new Upload();
			upload.setButtonCaption("Import");
			uploader = new FileUploder(upload, new ProgressBar(), new Label(), msg,
					unityServerConfiguration.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true),
					() -> importSchema());
			uploader.register();
			upload.setCaption(null);

			buttons.addComponent(upload);
			buttons.addComponent(add);

			schemasGrid = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
			schemasGrid.addComponentColumn(s -> StandardButtonsHelper.buildLinkButton(s.getId(), e -> gotoEdit(s)),
					msg.getMessage("SCIMServiceEditorSchemaTab.schemaId"), 20);
			schemasGrid.addCheckboxColumn(s -> s.isEnable(), msg.getMessage("SCIMServiceEditorSchemaTab.enabled"), 20);
			main.addComponent(schemasGrid);
		}

		private void importSchema()
		{
			try
			{
				SchemaWithMappingBean schema = ConfigurationVaadinBeanMapper.mapFromConfigurationSchema(
						SchemaResourceDeserialaizer.deserializeFromFile(uploader.getFile()));
				uploader.clear();
				if (schemasGrid.getElements().stream().filter(s -> s.getId().equals(schema.getId())).findAny()
						.isPresent())
				{
					NotificationPopup.showError(
							msg.getMessage("SCIMServiceEditorSchemaTab.schemaExistError", schema.getId()), "");
					return;
				}

				schemasGrid.addElement(schema);
				fireChange();
			} catch (IOException e)
			{
				uploader.clear();
				fireChange();
				NotificationPopup.showError(msg, "", e);
			}
		}

		private List<SingleActionHandler<SchemaWithMappingBean>> getActionsHandlers()
		{
			SingleActionHandler<SchemaWithMappingBean> edit = SingleActionHandler
					.builder4Edit(msg, SchemaWithMappingBean.class).withHandler(r ->
					{
						SchemaWithMappingBean edited = r.iterator().next();
						gotoEdit(edited);
					}

					).build();

			SingleActionHandler<SchemaWithMappingBean> remove = SingleActionHandler
					.builder4Delete(msg, SchemaWithMappingBean.class).withHandler(r ->
					{

						SchemaWithMappingBean schema = r.iterator().next();
						schemasGrid.removeElement(schema);

						fireChange();
					}).withDisabledPredicate(s -> isDefaultSchema(s)).build();

			return Arrays.asList(edit, remove);
		}

		private void gotoNew()
		{
			gotoEditSubView(null, s ->
			{
				subViewSwitcher.exitSubView();
				schemasGrid.addElement(s);
			});

		}

		private void gotoEdit(SchemaWithMappingBean edited)
		{
			gotoEditSubView(edited, s ->
			{
				schemasGrid.replaceElement(edited, s);
				subViewSwitcher.exitSubView();
			});
		}

		private void gotoEditSubView(SchemaWithMappingBean edited, Consumer<SchemaWithMappingBean> onConfirm)
		{
			EditSchemaSubView subView = new EditSchemaSubView(msg,
					schemasGrid.getElements().stream().map(s -> s.getId()).collect(Collectors.toList()), edited,
					isDefaultSchema(edited), s ->
					{
						onConfirm.accept(s);
						fireChange();
						schemasGrid.focus();
					}, () ->
					{
						subViewSwitcher.exitSubView();
						schemasGrid.focus();
					});
			subViewSwitcher.goToSubView(subView);
		}

		@Override
		public List<SchemaWithMappingBean> getValue()
		{
			return schemasGrid.getElements();
		}

		@Override
		protected Component initContent()
		{
			return main;
		}

		@Override
		protected void doSetValue(List<SchemaWithMappingBean> value)
		{
			schemasGrid.setItems(value);

		}

		private void fireChange()
		{
			fireEvent(new ValueChangeEvent<List<SchemaWithMappingBean>>(this, schemasGrid.getElements(), true));
		}

		private boolean isDefaultSchema(SchemaWithMappingBean schema)
		{
			return schema != null && DefaultSchemaProvider.isDefaultSchema(schema.getId());
		}

	}

}
