package com.scienceminer.nerd.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Utility methods for managing NERD properties.
 * 
 * @author Patrice
 * 
 */
public class NerdPropertiesUtil {

	/**
	 * Return a list of {@link NerdProperty} containing all the properties of
	 * Nerd.
	 * 
	 * @return List<NerdProperty>
	 */
	public static List<NerdProperty> getAllPropertiesList() {
		List<NerdProperty> gbdProperties = new ArrayList<NerdProperty>();
		Properties props = NerdServiceProperties.getProps();
		for (Object property : props.keySet()) {
			final String currProperty = (String) property;
			gbdProperties.add(new NerdProperty(currProperty, props.getProperty(currProperty)));
		}
		props = NerdProperties.getProps();
		for (Object property : props.keySet()) {
			final String currProperty = (String) property;
			gbdProperties.add(new NerdProperty(currProperty, props.getProperty(currProperty)));
		}
		return gbdProperties;
	}

	/**
	 * Build an xml representation of all grobid properties.
	 * 
	 * @return String
	 */
	public static String getAllPropertiesListXml() {
		StringBuilder gbdProperties = new StringBuilder();
		gbdProperties.append(XmlUtils.startTag("properties"));
		for (NerdProperty currProp : getAllPropertiesList()) {
			if (!NerdPropertyKeys.PROP_NERD_IS_CONTEXT_SERVER.equals(currProp.getKey())) {
				gbdProperties.append(currProp.toString());
			}
		}
		gbdProperties.append(XmlUtils.endTag("properties"));
		return gbdProperties.toString();
	}

	public static String getAllPropertiesListJson() {
		StringWriter sw = new StringWriter();
		ObjectMapper mapper = new ObjectMapper();
		try {
			mapper.writeValue(sw, getAllPropertiesList());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return sw.toString();
	}

}
