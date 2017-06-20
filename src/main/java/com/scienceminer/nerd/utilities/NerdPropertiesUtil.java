package com.scienceminer.nerd.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Utility methods for managing NERD properties.
 * 
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

}
