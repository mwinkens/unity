/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_8;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;
import pl.edu.icm.unity.store.objstore.endpoint.EndpointHandler;


@Component
public class InDBUpdateFromSchema14 implements InDBContentsUpdater
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_DB, InDBUpdateFromSchema14.class);
	
	@Autowired
	private ObjectStoreDAO genericObjectsDAO;
	
	@Override
	public int getUpdatedVersion()
	{
		return 14;
	}
	
	@Override
	public void update() throws IOException
	{
		updateOAuthEndpointConfiguration();
	}

	private void updateOAuthEndpointConfiguration()
	{
		List<GenericObjectBean> endpoints = genericObjectsDAO.getObjectsOfType(EndpointHandler.ENDPOINT_OBJECT_TYPE);
		for (GenericObjectBean endpoint : endpoints)
		{
			ObjectNode parsed = JsonUtil.parse(endpoint.getContents());
			String typeId = parsed.get("typeId").asText();	
			if ("OAuth2Authz".equals(typeId) || "OAuth2Token".equals(typeId))
			{
				ObjectNode configuration = (ObjectNode) parsed.get("configuration");
				JsonNode iconfiguration = configuration.get("configuration");
				JsonNode migratedConfig = new OAuthEndpointConfigurationMigrator(iconfiguration).migrate();
				configuration.set("configuration", migratedConfig);
				
				endpoint.setContents(JsonUtil.serialize2Bytes(parsed));
				LOG.info("Updating OAuth endpoint {} with id {}, \nold config: {}\nnew config: {}", 
						endpoint.getName(), endpoint.getId(), configuration, migratedConfig);
				genericObjectsDAO.updateByKey(endpoint.getId(), endpoint);
			}
		}		
	}
}
