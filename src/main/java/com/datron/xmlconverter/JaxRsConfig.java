package com.datron.xmlconverter;

import org.glassfish.jersey.server.ServerProperties;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


@ApplicationPath("/api")
public class JaxRsConfig extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		final Set<Class<?>> classes = new HashSet<Class<?>>();

		// prevent registering all providers at utils package
		registerResources(classes);

//		classes.add(GZIPWriterInterceptor.class);
//		classes.add(RestExceptionMapper.class);

		// my current java ee / glassfish / jersey preference
//		classes.add(MoxyJsonFeature.class);

		return classes;
	}

	private void registerResources(Set<Class<?>> classes) {
		// this does not work for spring boot fat jars / only for war packaging
//		packages(true, this.getClass().getPackage().getName());
		// for jar needs to register resources separately
		classes.add(BaseResource.class);
	}

	@Override
	public Map<String, Object> getProperties() {
		final Map<String, Object> properties = new HashMap<>();
		// my current java ee / glassfish / jersey preference
		properties.put(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
		return properties;
	}
}
