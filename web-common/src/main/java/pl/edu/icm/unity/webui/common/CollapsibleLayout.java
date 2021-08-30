/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.common;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * Layout wrapper with collapse/expand button
 * @author P.Piernik
 *
 */
public class CollapsibleLayout extends CustomComponent
{
	private VerticalLayout contentWrapper;
	private Button modeButton;
	private VerticalLayout main;
	private Label captionLabel;		
	public CollapsibleLayout(String caption, Component content)
	{
		HorizontalLayout bar = new HorizontalLayout();
		bar.setMargin(false);
		bar.setSpacing(true);
		bar.setWidth(100, Unit.PERCENTAGE);
		bar.addLayoutClickListener(e -> changeMode());
		
		modeButton = new Button();
		modeButton.addStyleName(Styles.vButtonLink.toString());
		modeButton.addStyleName(Styles.showHideButton.toString());
		modeButton.addClickListener(e -> changeMode());
	
		captionLabel = new Label(caption);
		
		HorizontalLayout buttonWithCaption = new HorizontalLayout();
		buttonWithCaption.setMargin(false);
		buttonWithCaption.setSpacing(false);
		buttonWithCaption.addComponents(modeButton, captionLabel);
	
		Label line = HtmlTag.horizontalLineBold();
		
		bar.addComponents(buttonWithCaption, line);
		bar.setExpandRatio(buttonWithCaption, 0);
		bar.setExpandRatio(line, 2);
		bar.setComponentAlignment(buttonWithCaption, Alignment.TOP_LEFT);
		bar.setComponentAlignment(line, Alignment.BOTTOM_RIGHT);
		
		contentWrapper = new VerticalLayout();
		contentWrapper.addComponent(content);
		
		main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		main.addComponents(bar, contentWrapper);
		
		collapse();
		
		setCompositionRoot(main);	
	}
	
	public void setTitle(String title)
	{
		captionLabel.setCaption(title);
	}
	
	public void setMargin(MarginInfo margin)
	{
		main.setMargin(margin);
	}
	
	private void changeMode()
	{
		if (contentWrapper.isVisible())
		{
			collapse();
		}else
		{
			expand();
		}
	}
	
	public void collapse()
	{
		contentWrapper.setVisible(false);
		modeButton.setIcon(Images.caret_right.getResource());
	}
	
	public void expand()
	{
		contentWrapper.setVisible(true);
		modeButton.setIcon(Images.caret_down.getResource());
	}
}
