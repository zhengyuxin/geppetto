/**
 * Copyright (c) 2013 Puppet Labs, Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Puppet Labs
 */
package com.puppetlabs.geppetto.pp.dsl.eval;

import java.util.Collections;

import com.puppetlabs.geppetto.pp.AdditiveExpression;
import com.puppetlabs.geppetto.pp.AndExpression;
import com.puppetlabs.geppetto.pp.AtExpression;
import com.puppetlabs.geppetto.pp.DoubleQuotedString;
import com.puppetlabs.geppetto.pp.EqualityExpression;
import com.puppetlabs.geppetto.pp.FunctionCall;
import com.puppetlabs.geppetto.pp.LiteralBoolean;
import com.puppetlabs.geppetto.pp.LiteralHash;
import com.puppetlabs.geppetto.pp.LiteralList;
import com.puppetlabs.geppetto.pp.LiteralName;
import com.puppetlabs.geppetto.pp.LiteralNameOrReference;
import com.puppetlabs.geppetto.pp.LiteralUndef;
import com.puppetlabs.geppetto.pp.MatchingExpression;
import com.puppetlabs.geppetto.pp.MultiplicativeExpression;
import com.puppetlabs.geppetto.pp.OrExpression;
import com.puppetlabs.geppetto.pp.ParenthesisedExpression;
import com.puppetlabs.geppetto.pp.RelationalExpression;
import com.puppetlabs.geppetto.pp.SelectorExpression;
import com.puppetlabs.geppetto.pp.ShiftExpression;
import com.puppetlabs.geppetto.pp.SingleQuotedString;
import com.puppetlabs.geppetto.pp.TextExpression;
import com.puppetlabs.geppetto.pp.UnaryMinusExpression;
import com.puppetlabs.geppetto.pp.UnaryNotExpression;
import com.puppetlabs.geppetto.pp.VariableExpression;
import com.puppetlabs.geppetto.pp.VerbatimTE;
import com.puppetlabs.geppetto.pp.util.TextExpressionHelper;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.util.PolymorphicDispatcher;

/**
 * Evaluates Type of an expression.
 * 
 */
public class PPTypeEvaluator {
	public enum PPType {
		/** type can not be determined statically */
		DYNAMIC,

		/** expression results in a string that is known to have numeric content */
		NUMERIC,

		/** expression results in a string that is known to be not numeric */
		STRING,

		/** expression results in a string that may or may not be numeric */
		DYNAMIC_STRING,

		/** expression results in boolean */
		BOOLEAN,

		/** expression result is a regexp */
		REGEXP,

		/** expression results in a list */
		LIST,

		/** expression results in a hash */
		HASH,

		/** expression is literal undefined */
		UNDEF,

		/** non value producing expression (procedure, or statement) */
		VOID,
	}

	public enum TypeConformant {
		YES, NO, INCONCLUSIVE;

		public boolean maybeConformant(TypeConformant tc) {
			return tc == YES || tc == INCONCLUSIVE;
		}
	}

	private PolymorphicDispatcher<PPType> typeDispatcher = new PolymorphicDispatcher<PPType>(
		"_type", 1, 1, Collections.singletonList(this), PolymorphicDispatcher.NullErrorHandler.<PPType> get()) {
		@Override
		protected PPType handleNoSuchMethod(Object... params) {
			return PPType.VOID;
		}
	};

	protected PPType _type(AdditiveExpression o) {
		return PPType.NUMERIC;
	}

	protected PPType _type(AndExpression o) {
		return PPType.BOOLEAN;
	}

	protected PPType _type(AtExpression o) {
		return PPType.DYNAMIC;
	}

	protected PPType _type(DoubleQuotedString o) {
		if(!TextExpressionHelper.hasInterpolation(o)) {
			// if there is no interpolation, there is by definition only one StringData
			// ignore the fact that a static string could be interpolated since this is an
			// esoteric case.
			// TODO: Handle this esoteric case "abc${"xyz"}"
			EList<TextExpression> parts = o.getStringPart();
			if(parts.size() < 1)
				return PPType.STRING;
			if(parts.size() == 1) {
				return isNumericString(((VerbatimTE) parts.get(0)).getText())
						? PPType.NUMERIC
						: PPType.STRING;
			}
			throw new IllegalStateException("hasInterpolation() returned false, but there was interpolation");
		}
		// there is interpolation
		// Can check if impossible that it is numeric
		// A simplistic check can replace each interpolated expression with 0 and see if the result is numeric.
		// This leaves a coupel of esotric cases where someone tries to compose a hex or floating point number and
		// the interpolated values are for the 'x' 'X', 'E', or '+'/'-'
		//
		StringBuilder builder = new StringBuilder();
		for(TextExpression te : o.getStringPart()) {
			if(te instanceof VerbatimTE)
				builder.append(((VerbatimTE) te).getText());
			else
				builder.append("0");
		}
		// i.e. either dynamic string that *may* be a number, or a non numeric string otherwise
		return isNumericString(builder.toString())
				? PPType.DYNAMIC_STRING
				: PPType.STRING;
	}

	protected PPType _type(EqualityExpression o) {
		return PPType.BOOLEAN;
	}

	protected PPType _type(FunctionCall o) {
		return PPType.DYNAMIC;
	}

	protected PPType _type(LiteralBoolean o) {
		return PPType.BOOLEAN;
	}

	protected PPType _type(LiteralHash o) {
		return PPType.HASH;
	}

	protected PPType _type(LiteralList o) {
		return PPType.LIST;
	}

	protected PPType _type(LiteralName o) {
		return isNumericString(o.getValue())
				? PPType.NUMERIC
				: PPType.STRING;
	}

	protected PPType _type(LiteralNameOrReference o) {
		return isNumericString(o.getValue())
				? PPType.NUMERIC
				: PPType.STRING;
	}

	protected PPType _type(LiteralUndef o) {
		return PPType.UNDEF;
	}

	protected PPType _type(MatchingExpression o) {
		return PPType.BOOLEAN;
	}

	protected PPType _type(MultiplicativeExpression o) {
		return PPType.NUMERIC;
	}

	protected PPType _type(OrExpression o) {
		return PPType.BOOLEAN;
	}

	protected PPType _type(ParenthesisedExpression o) {
		return type(o.getExpr());
	}

	protected PPType _type(RelationalExpression o) {
		return PPType.BOOLEAN;
	}

	protected PPType _type(SelectorExpression o) {
		return PPType.DYNAMIC;
	}

	protected PPType _type(ShiftExpression o) {
		return PPType.NUMERIC;
	}

	protected PPType _type(SingleQuotedString o) {
		return isNumericString(o.getText())
				? PPType.NUMERIC
				: PPType.STRING;
	}

	protected PPType _type(UnaryMinusExpression o) {
		return PPType.NUMERIC;
	}

	protected PPType _type(UnaryNotExpression o) {
		return PPType.BOOLEAN;
	}

	protected PPType _type(VariableExpression o) {
		return PPType.DYNAMIC;
	}

	private boolean isNumericString(String s) {
		try {
			Integer.parseInt(s, 10);
			return true;
		}
		catch(NumberFormatException e) {
			// not decimal
		}
		try {
			Integer.parseInt(s, 8);
			return true;
		}
		catch(NumberFormatException e) {
			// not octal
		}
		try {
			String tmp = s;
			if(s.startsWith("-"))
				tmp = s.substring(1);
			if(tmp.startsWith("0x") || tmp.startsWith("0X")) {
				Integer.parseInt(tmp.substring(2), 16);
				return true;
			}
		}
		catch(NumberFormatException e) {
			// not hexadecimal
		}
		try {
			Double.valueOf(s);
			return true;
		}
		catch(NumberFormatException e) {
			// not floating point
		}

		return false; // not numeric
	}

	public TypeConformant numeric(PPType t) {
		if(t == PPType.NUMERIC)
			return TypeConformant.YES;
		if(t == PPType.DYNAMIC || t == PPType.DYNAMIC_STRING)
			return TypeConformant.INCONCLUSIVE;
		return TypeConformant.NO;
	}

	public TypeConformant numericType(Object o) {
		return numeric(type(o));
	}

	public PPType type(Object o) {
		if(o == null)
			return PPType.VOID;
		return typeDispatcher.invoke(o);

	}
}
