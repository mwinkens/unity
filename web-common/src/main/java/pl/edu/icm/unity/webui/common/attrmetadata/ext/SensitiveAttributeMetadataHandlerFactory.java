/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attrmetadata.ext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.stdext.utils.SensitiveAttributeMetadataProvider;
import pl.edu.icm.unity.webui.common.attrmetadata.WebAttributeMetadataHandler;
import pl.edu.icm.unity.webui.common.attrmetadata.WebAttributeMetadataHandlerFactory;


@Component
public class SensitiveAttributeMetadataHandlerFactory implements WebAttributeMetadataHandlerFactory
{
	private MessageSource msg;
	
	@Autowired
	public SensitiveAttributeMetadataHandlerFactory(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedMetadata()
	{
		return SensitiveAttributeMetadataProvider.NAME;
	}

	@Override
	public WebAttributeMetadataHandler newInstance()
	{
		return new SensitiveAttributeMetadataHandler(msg);
	}
}
