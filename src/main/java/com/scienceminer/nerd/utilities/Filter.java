package com.scienceminer.nerd.utilities;

import java.util.*;
import java.util.regex.*;

import com.scienceminer.nerd.kb.Property;
import com.scienceminer.nerd.kb.Statement;

/**
 * Semantic filter on entities with criteria expressed on the statements of the entity.
 */
public class Filter {

	private Property property = null;
	private String valueMustMatch = null;
	private Pattern valueMustMatchPattern = null;
	private String valueMustNotMatch = null;
	private Pattern valueMustNotMatchPattern = null;

	public Property getProperty() {
		return this.property;
	}

	public void setProperty(Property property) {
		this.property = property;
	}

	public String getValueMustMatch() {
		return this.valueMustMatch;
	}

	public void setValueMustMatch(String valueMustMatch) {
		this.valueMustMatch = valueMustMatch;
	}

	public String getValueMustNotMatch() {
		return this.valueMustNotMatch;
	}

	public void setValueMustNotMatch(String valueMustNotMatch) {
		this.valueMustNotMatch = valueMustNotMatch;
	}

	public boolean valid(List<Statement> statements) {
		boolean validity = false;
		if (property == null) 
			return true;
		if ( (statements == null) || (statements.size() == 0) )
			return true;
		for(Statement statement : statements) {
			if (statement.getProperty() != null) {
				if (statement.getProperty().getId().equals(property.getId()) ) {
					if ( (valueMustMatch == null) || (valueMustMatch.equals("*")) ) {
						return true;
					} else {
						if (valueMustMatchPattern == null) {
							try {
 								valueMustMatchPattern =  Pattern.compile(valueMustMatch);
 							} catch(Exception e) {
 								e.printStackTrace();
 							}
						}
						if (valueMustNotMatchPattern == null) {
							try {
 								valueMustNotMatchPattern =  Pattern.compile(valueMustNotMatch);
 							} catch(Exception e) {
 								e.printStackTrace();
 							}
						}

						if (valueMustMatchPattern != null) {
							 Matcher m = valueMustMatchPattern.matcher(statement.getValue());
							 if (m.matches())
							 	return true;
						}

						if (valueMustNotMatchPattern != null) {
							Matcher m = valueMustNotMatchPattern.matcher(statement.getValue());
							if (!m.matches())
						 		return true;						 
						}
					}
				}
			}
		}

		return validity;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (property != null)
			builder.append(property.getId() + "\t");
		if (valueMustMatch != null)
			builder.append(valueMustMatch + "\t");
		if (valueMustNotMatch != null)	
			builder.append(valueMustNotMatch);
		return builder.toString();
	}
}