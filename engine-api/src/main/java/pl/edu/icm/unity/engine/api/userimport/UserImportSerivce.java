/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.userimport;

import java.util.List;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Internal API for triggering user import.
 * 
 * @author Krzysztof Benedyczak
 */
public interface UserImportSerivce
{
	/**
	 * Perform user import of a user which has to be mapped and or created with regular way 
	 * (same as during authentication) by the input profile
	 * @param identity
	 * @param type
	 * @return the returned object can be typically ignored, but is provided if caller is interested in 
	 * the results of import. The object's class is bit misnamed here, but it provides the complete information
	 * which is the same as after remote authentication of a user. 
	 */
	List<AuthenticationResult> importUser(List<UserImportSpec> imports);

	/**
	 * Performs an import which enriches the information about the existing user. 
	 * The input profile need not to have any mapIdentity actions, as all operations will be performed
	 * in the context of the given user.
	 * 
	 * @param imports
	 * @return
	 */
	List<AuthenticationResult> importToExistingUser(List<UserImportSpec> imports, 
			IdentityTaV existingUser);
}
