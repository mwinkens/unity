/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.scim.group;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.imunity.scim.SCIMConstants;
import io.imunity.scim.SCIMEndpoint;
import io.imunity.scim.SCIMRestController;
import io.imunity.scim.SCIMRestControllerFactory;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.group.SCIMGroupResourceAssemblyService.SCIMGroupResourceAssemblyServiceFactory;
import io.imunity.scim.group.SCIMGroupRetrievalService.SCIMGroupRetrievalServiceFactory;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;

@Produces(MediaType.APPLICATION_JSON)
@Path(SCIMEndpoint.PATH)
public class SCIMGroupRestController implements SCIMRestController
{
	public static final String SINGLE_GROUP_LOCATION = "/Group";

	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, SCIMGroupRestController.class);

	private final ObjectMapper mapper = SCIMConstants.MAPPER;

	private final SCIMGroupRetrievalService groupRetrievalService;
	private final SCIMGroupResourceAssemblyService groupAssemblyService;

	SCIMGroupRestController(SCIMGroupRetrievalService groupRetrievalService,
			SCIMGroupResourceAssemblyService groupAssemblyService)
	{

		this.groupRetrievalService = groupRetrievalService;
		this.groupAssemblyService = groupAssemblyService;
	}

	@Path("/Groups")
	@GET
	public String getGroups() throws EngineException, JsonProcessingException
	{
		log.debug("Get groups");
		return mapper.writeValueAsString(groupAssemblyService.mapToGroupsResource(groupRetrievalService.getGroups()));
	}

	@Path(SINGLE_GROUP_LOCATION + "/{id}")
	@GET
	public String getGroup(@PathParam("id") String groupId) throws EngineException, JsonProcessingException
	{
		log.debug("Get group with id: {}", groupId);
		return mapper.writeValueAsString(
				groupAssemblyService.mapToGroupResource(groupRetrievalService.getGroup(new GroupId(groupId))));
	}

	@Component
	static class SCIMGroupRestControllerFactory implements SCIMRestControllerFactory
	{
		private final SCIMGroupRetrievalServiceFactory retServiceFactory;
		private final SCIMGroupResourceAssemblyServiceFactory assemblyServiceFactory;

		@Autowired
		SCIMGroupRestControllerFactory(SCIMGroupRetrievalServiceFactory retServiceFactory,
				SCIMGroupResourceAssemblyServiceFactory assemblyServiceFactory)
		{
			this.retServiceFactory = retServiceFactory;
			this.assemblyServiceFactory = assemblyServiceFactory;
		}

		@Override
		public SCIMGroupRestController getController(SCIMEndpointDescription configuration)
		{
			return new SCIMGroupRestController(retServiceFactory.getService(configuration),
					assemblyServiceFactory.getService(configuration));
		}
	}

}
