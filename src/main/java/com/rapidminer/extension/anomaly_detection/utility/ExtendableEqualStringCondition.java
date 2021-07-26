package com.rapidminer.extension.anomaly_detection.utility;

import org.w3c.dom.Element;

import com.rapidminer.io.process.XMLTools;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualStringCondition;
import com.rapidminer.parameter.conditions.ParameterCondition;
import com.rapidminer.tools.XMLException;


public class ExtendableEqualStringCondition extends ParameterCondition {

	private static final String ELEMENT_VALUES = "Values";
	private static final String ELEMENT_VALUE = "Value";
	private String[] types;

	public ExtendableEqualStringCondition(Element element) throws XMLException {
		super(element);
		Element valuesElement = XMLTools.getChildElement(element, ELEMENT_VALUES, true);
		types = XMLTools.getChildTagsContentAsStringArray(valuesElement, ELEMENT_VALUE);
	}

	public ExtendableEqualStringCondition(ParameterHandler handler, String conditionParameter, boolean becomeMandatory,
								String... types) {
		super(handler, conditionParameter, becomeMandatory);
		this.types = types;
	}

	@Override
	public boolean isConditionFullfilled() {
		boolean equals = false;
		String isType;
		try {
			isType = parameterHandler.getParameterAsString(conditionParameter);
		} catch (UndefinedParameterError e) {
			return false;
		}
		for (String type : types) {
			equals |= type.equals(isType);
		}
		return equals;
	}

	public void addType(String newType) {
		String[] newTypes = new String[types.length+1];
		int i = 0;
		for(String oldString : types) {
			newTypes[i] = types[i];
			++i;
		}
		newTypes[types.length] = newType;
		types = newTypes;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (types.length > 1) {
			builder.append(conditionParameter.replace('_', ' ') + " \u2208 {");
			for (int i = 0; i < types.length; i++) {
				builder.append(types[i]);
				if (i + 1 < types.length) {
					builder.append(", ");
				}
			}
			builder.append("}");
		} else {
			if (types.length > 0) {
				builder.append(conditionParameter.replace('_', ' ') + " = " + types[0]);
			}
		}
		return builder.toString();
	}

	@Override
	public void getDefinitionAsXML(Element element) {
		Element valuesElement = XMLTools.addTag(element, ELEMENT_VALUES);
		for (String value : types) {
			XMLTools.addTag(valuesElement, ELEMENT_VALUE, value);
		}
	}
}


