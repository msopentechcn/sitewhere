/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.groovy.device.provisioning;

import groovy.lang.Binding;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.sitewhere.groovy.GroovyConfiguration;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.device.provisioning.IDecodedDeviceEventRequest;
import com.sitewhere.spi.device.provisioning.IDeviceEventDecoder;

/**
 * Implementation of {@link IDeviceEventDecoder} that delegates parsing to a Groovy
 * script.
 * 
 * @author Derek
 */
public class GroovyStringEventDecoder implements IDeviceEventDecoder<String> {

	/** Static logger instance */
	private static Logger LOGGER = Logger.getLogger(GroovyStringEventDecoder.class);

	/** Groovy variable used for decoded events */
	private static final String VAR_DECODED_EVENTS = "events";

	/** Groovy variable used for passing payload */
	private static final String VAR_PAYLOAD = "payload";

	/** Groovy variable used for passing logger */
	private static final String VAR_LOGGER = "logger";

	/** Injected global Groovy configuration */
	private GroovyConfiguration configuration;

	/** Path to script used for decoder */
	private String scriptPath;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sitewhere.spi.device.provisioning.IDeviceEventDecoder#decode(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<IDecodedDeviceEventRequest> decode(String payload) throws SiteWhereException {
		try {
			Binding binding = new Binding();
			List<IDecodedDeviceEventRequest> events = new ArrayList<IDecodedDeviceEventRequest>();
			binding.setVariable(VAR_DECODED_EVENTS, events);
			binding.setVariable(VAR_PAYLOAD, payload);
			binding.setVariable(VAR_LOGGER, LOGGER);
			LOGGER.info("About to execute '" + getScriptPath() + "' with payload: " + payload);
			getConfiguration().getGroovyScriptEngine().run(getScriptPath(), binding);
			return (List<IDecodedDeviceEventRequest>) binding.getVariable(VAR_DECODED_EVENTS);
		} catch (ResourceException e) {
			throw new SiteWhereException("Unable to access Groovy decoder script.", e);
		} catch (ScriptException e) {
			throw new SiteWhereException("Unable to run Groovy decoder script.", e);
		}
	}

	public GroovyConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(GroovyConfiguration configuration) {
		this.configuration = configuration;
	}

	public String getScriptPath() {
		return scriptPath;
	}

	public void setScriptPath(String scriptPath) {
		this.scriptPath = scriptPath;
	}
}