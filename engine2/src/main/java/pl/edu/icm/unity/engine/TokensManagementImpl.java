/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.utils.TimeUtil;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.api.TokenDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Implementation of {@link TokensManagement}
 * 
 * @author K. Benedyczak
 */
@Component
public class TokensManagementImpl implements TokensManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, TokensManagementImpl.class);
	private EntityResolver idResolver;
	private TokenDAO dbTokens;
	private TransactionalRunner tx; 
	private Map<String, List<TokenExpirationListener>> listeners = 
			new HashMap<String, List<TokenExpirationListener>>();
	
	@Autowired
	public TokensManagementImpl(EntityResolver idResolver, TransactionalRunner tx,
			TokenDAO dbTokens, ExecutorsService executorsService)
	{
		this.idResolver = idResolver;
		this.tx = tx;
		this.dbTokens = dbTokens;
		
		Runnable cleaner = new Runnable()
		{
			@Override
			public void run()
			{
				removeExpired();
			}
		};
		executorsService.getService().scheduleWithFixedDelay(cleaner, 30, 60, TimeUnit.SECONDS);
	}

	@Transactional
	@Override
	public void addToken(String type, String value, EntityParam owner, byte[] contents,
			Date created, Date expires) 
			throws WrongArgumentException, IllegalIdentityValueException, IllegalTypeException
	{
		long entity = idResolver.getEntityId(owner);
		addTokenInternal(type, value, contents, created, expires, entity);
	}
	
	@Transactional
	@Override
	public void addToken(String type, String value, byte[] contents,
			Date created, Date expires) 
			throws WrongArgumentException, IllegalTypeException
	{
		addTokenInternal(type, value, contents, created, expires, null);
	}
	
	private void addTokenInternal(String type, String value, byte[] contents,
			Date created, Date expires, Long entity)
	{
		Token token = new Token(type, value, entity);
		token.setContents(contents);
		token.setCreated(TimeUtil.roundToS(created));
		token.setExpires(TimeUtil.roundToS(expires));
		dbTokens.create(token);
	}
	
	@Transactional
	@Override
	public void removeToken(String type, String value) throws WrongArgumentException
	{
		dbTokens.delete(type, value);
	}

	@Transactional
	@Override
	public void updateToken(String type, String value, Date expires, byte[] contents)
			throws WrongArgumentException
	{
		Token token = new Token(type, value, null);
		token.setContents(contents);
		token.setExpires(TimeUtil.roundToS(expires));		
		dbTokens.update(token);
	}

	@Transactional(autoCommit=false)
	@Override
	public Token getTokenById(String type, String value) throws WrongArgumentException
	{
		Token token = dbTokens.get(type, value);
		if (token.isExpired())
			throw new WrongArgumentException("There is no such token");
		return token;
	}
	
	@Transactional
	@Override
	public List<Token> getOwnedTokens(String type, EntityParam owner) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		long entity = idResolver.getEntityId(owner);
		return dbTokens.getOwned(type, entity);
	}
	
	@Transactional
	@Override
	public List<Token> getAllTokens(String type)
	{
		List<Token> tokens = dbTokens.getByType(type);
		List<Token> ret = new ArrayList<>(tokens.size());;
		for (Token t: tokens)
			if (!t.isExpired())
				ret.add(t);
		return ret;
	}

	@Override
	public synchronized void addTokenExpirationListener(TokenExpirationListener listener, String type)
	{
		List<TokenExpirationListener> l = listeners.get(type);
		if (l == null)
		{
			l = new ArrayList<>();
			listeners.put(type, l);
		}
		l.add(listener);
	}
	
	private synchronized void removeExpired()
	{
		tx.runInTransaction(() -> {
			transactionalRemoveExpired();
		});
	}

	private void transactionalRemoveExpired()
	{
		log.debug("Removing expired tokens");
		int removed = 0;
		
		List<Token> tokens = dbTokens.getExpired();
		for (Token t: tokens)
		{
			List<TokenExpirationListener> l = listeners.get(t.getType());
			if (l != null)
			{
				for (TokenExpirationListener listener: l)
					listener.tokenExpired(t);
			}
			try
			{
				dbTokens.delete(t.getType(), t.getValue());
				removed++;
			} catch (Exception e)
			{
				log.error("Problem removing an expired token [" + t.getType() +
						"] " + t.getValue(), e);
			}
		}
		log.debug("Removed " + removed + " tokens in this round");
	}
}
