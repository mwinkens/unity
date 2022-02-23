/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.rest.jwt.JWTAuthenticationProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;

/**
 * Extension of SAML SP properties. Allows for specification of the settings required to generate 
 * JWT after completed ECP flow.
 *  
 * @author K. Benedyczak
 */
public class SAMLECPProperties extends SAMLSPProperties
{
	public static final String JWT_P = "jwt.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> ECP_META = new HashMap<>();
	
	static
	{
		DocumentationCategory jwt = new DocumentationCategory(
				"JWT generation specific settings", "04");

		for (Map.Entry<String, PropertyMD> entry: SAMLSPProperties.META.entrySet())
		{
			if (entry.getKey().equals(JWT_P))
				continue;
			ECP_META.put(entry.getKey(), entry.getValue());
		}
		for (Map.Entry<String, PropertyMD> entry: JWTAuthenticationProperties.META.entrySet())
		{
			if (entry.getKey().equals(JWT_P))
				continue;
			ECP_META.put(JWT_P + entry.getKey(), entry.getValue().setCategory(jwt));
		}
	}
	
	public SAMLECPProperties(Properties properties, PKIManagement pkiMan) throws ConfigurationException
	{
		super(properties, ECP_META, pkiMan);
	}
	
	public JWTAuthenticationProperties getJWTProperties()
	{
		return new JWTAuthenticationProperties(P + JWT_P, properties);
	}
}
