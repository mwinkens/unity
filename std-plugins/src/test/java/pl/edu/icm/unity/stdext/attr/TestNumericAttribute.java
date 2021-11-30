/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;

public class TestNumericAttribute
{
	@Test
	public void shouldValidateMinInRange() throws Exception
	{
		IntegerAttributeSyntax ias = new IntegerAttributeSyntax();
		ias.setMin(-33);

		ias.validate(-33L);
	}

	@Test
	public void shouldFailMinOutOfRange() throws Exception
	{
		IntegerAttributeSyntax ias = new IntegerAttributeSyntax();
		ias.setMin(-33);

		Throwable error = catchThrowable(() -> ias.validate(-34L));
		
		assertThat(error).isInstanceOf(IllegalAttributeValueException.class);
	}

	@Test
	public void shouldValidateMaxInRange() throws Exception
	{
		IntegerAttributeSyntax ias = new IntegerAttributeSyntax();
		ias.setMax(12);

		ias.validate(12l);
	}

	@Test
	public void shouldFailMaxOutOfRange() throws Exception
	{
		IntegerAttributeSyntax ias = new IntegerAttributeSyntax();
		ias.setMax(12);

		Throwable error = catchThrowable(() -> ias.validate(13L));
		
		assertThat(error).isInstanceOf(IllegalAttributeValueException.class);
	}

	@Test
	public void conversionToStringIsIdempotent() throws Exception
	{
		IntegerAttributeSyntax ias = new IntegerAttributeSyntax();
		
		long before = 123123123123L;
		String s = ias.convertToString(before);
		long after = ias.convertFromString(s);
		assertEquals(before, after);
	}
	
	@Test
	public void equalityWorksOnNumbers() throws Exception
	{
		IntegerAttributeSyntax ias = new IntegerAttributeSyntax();
		
		assertTrue(ias.areEqual(1234L, 1234l));
		assertFalse(ias.areEqual(1235L, 1234l));
	}
	
	@Test
	public void serializationIsIdempotent() throws Exception
	{
		IntegerAttributeSyntax ias = new IntegerAttributeSyntax();
		ias.setMin(-33);
		ias.setMax(12);
		
		JsonNode cfg = ias.getSerializedConfiguration();
		IntegerAttributeSyntax ias2 = new IntegerAttributeSyntax();
		ias2.setSerializedConfiguration(cfg);
		
		assertEquals(ias2.getMax(), 12);
		assertEquals(ias2.getMin(), -33);
	}

	@Test
	public void testFloating() throws Exception
	{
		FloatingPointAttributeSyntax ias = new FloatingPointAttributeSyntax();
		ias.setMax(12.5);
		ias.setMin(-33.88);

		ias.validate(12.5);
		ias.validate(-33.88);
		
		try
		{
			ias.validate(12.6);
			fail("Added out of bounds value");
		} catch (IllegalAttributeValueException e) {}

		try
		{
			ias.validate(-33.89);
			fail("Added out of bounds value");
		} catch (IllegalAttributeValueException e) {}
		
		double before = 123123123123L;
		String s = ias.convertToString(before);
		double after = ias.convertFromString(s);
		assertEquals(before, after, 0.01);

		assertTrue(ias.areEqual(1234.3, Double.valueOf(1234.3)));
		assertFalse(ias.areEqual(1235.4, Double.valueOf(1234.3d)));
		
		JsonNode cfg = ias.getSerializedConfiguration();
		
		FloatingPointAttributeSyntax ias2 = new FloatingPointAttributeSyntax();
		ias2.setSerializedConfiguration(cfg);
		assertEquals(ias2.getMax(), 12.5, 0);
		assertEquals(ias2.getMin(), -33.88, 0);
	}
}
