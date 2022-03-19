package com.datron.xmlconverter;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Named;
import javax.ws.rs.ApplicationPath;

@Named
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {

	public JerseyConfig() {
		JaxRsConfig jaxRsConfig = new JaxRsConfig();
		addProperties(jaxRsConfig.getProperties());
		registerClasses(jaxRsConfig.getClasses());

		register(JacksonFeature.class);
		register(MultiPartFeature.class);
	}


}
