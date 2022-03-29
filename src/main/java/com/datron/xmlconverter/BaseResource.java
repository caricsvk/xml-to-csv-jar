package com.datron.xmlconverter;

import com.opencsv.CSVWriter;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/")
public class BaseResource {

	@GET
	public Response basicTest1() {
		return Response.ok("okej").build();
	}

	@POST
	@Path("parse")
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	public Response uploadPdfFile(@FormDataParam("file") InputStream fileInputStream,
								  @FormDataParam("file") FormDataContentDisposition fileMetaData) throws Exception {
		try {
			System.out.println("BaseResource.uploadPdfFile started");
			long start = System.currentTimeMillis();
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringElementContentWhitespace(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			System.out.println("BaseResource.uploadPdfFile before dom parse took " + (System.currentTimeMillis() - start) + "ms");
			long checkpointTime = System.currentTimeMillis();
			Document doc = builder.parse(fileInputStream);
			System.out.println("BaseResource.uploadPdfFile dom parse took " + (System.currentTimeMillis() - checkpointTime) + "ms");
			checkpointTime = System.currentTimeMillis();
			Element element = doc.getDocumentElement();
			LinkedHashSet<String> keys = findKeys(element.getChildNodes(), new LinkedHashSet<>());
			System.out.println("BaseResource.uploadPdfFile getting keys took " + (System.currentTimeMillis() - checkpointTime) + "ms");
			checkpointTime = System.currentTimeMillis();
			NodeList childNodes = element.getChildNodes().item(0).getChildNodes().item(0).getChildNodes();
			List<Map<String, String>> results = parse(childNodes, null, new ArrayList<>());
			System.out.println("BaseResource.uploadPdfFile parsing took " + (System.currentTimeMillis() - checkpointTime) + "ms");
			checkpointTime = System.currentTimeMillis();
			StreamingOutput output = outputStream -> {
				try (
						Writer writer = new OutputStreamWriter(outputStream);
						CSVWriter csvWriter = new CSVWriter(writer,
							CSVWriter.DEFAULT_SEPARATOR,
							CSVWriter.DEFAULT_QUOTE_CHARACTER,
							CSVWriter.DEFAULT_ESCAPE_CHARACTER,
							CSVWriter.DEFAULT_LINE_END);
				) {
					csvWriter.writeNext(keys.stream().map(key -> key.substring(69)).toArray(String[]::new));
					results.forEach(line ->
						csvWriter.writeNext(keys.stream().map(key -> line.getOrDefault(key, " ")).toArray(String[]::new))
					);
				}
			};
			System.out.println("BaseResource.uploadPdfFile creating csv took " + (System.currentTimeMillis() - checkpointTime) + "ms");
			return Response.status(200)
					.entity(output)
					.header("Content-Disposition", "attachment; filename=" + fileMetaData.getFileName() + ".csv")
					.build();
		} catch (Exception e) {
			Logger.getAnonymousLogger().log(Level.SEVERE, "caught error: " + e.getMessage(), e);
			throw new WebApplicationException("Error while uploading file. Please try again !!");
		}
//		return Response.ok("Data uploaded successfully !!").build();
	}

	public LinkedHashSet<String> findKeys(NodeList childNodes, LinkedHashSet<String> keys) {
		boolean skipNext = false;
		for (int i = 0; i < childNodes.getLength(); i ++) {
			if (skipNext) {
				skipNext = false;
				continue;
			}
			Node item = childNodes.item(i);
			if (item.getChildNodes().getLength() == 1 && item.getChildNodes().item(0).getChildNodes().getLength() == 0) {
				if (getName(item).contains("typ:parameter") || "stk:intParameterName".equals(item.getNodeName())) {
					skipNext = true;
					keys.add(getName(item) + "." + item.getTextContent());
				} else {
					keys.add(getName(item));
				}
			} else {
				findKeys(item.getChildNodes(), keys);
			}
		}
		return keys;
	}

	public List<Map<String, String>> parse(NodeList childNodes, HashMap<String, String> map, List<Map<String, String>> results) {
		boolean skipNext = false;
		for (int i = 0; i < childNodes.getLength(); i ++) {
			if (skipNext) {
				skipNext = false;
				continue;
			}
			Node item = childNodes.item(i);
			if (item.getChildNodes().getLength() == 1 && item.getChildNodes().item(0).getChildNodes().getLength() == 0) {
				if (getName(item).contains("typ:parameter")) {
					skipNext = true;
					addToMap(map, getName(item) + "." + item.getTextContent(), item.getNextSibling().getTextContent());
				} if ("stk:intParameterName".equals(item.getNodeName())) {
					skipNext = true;
					NodeList parameterValues = item.getParentNode().getLastChild().getChildNodes();
					List<String> values = new ArrayList<>();
					for (int j = 0; j < parameterValues.getLength(); j ++) {
						values.add(parameterValues.item(j).getTextContent());
					}
					addToMap(map, getName(item) + "." + item.getTextContent(), String.join(",", values));
				} else {
					addToMap(map, getName(item), item.getTextContent());
				}
			} else if (map == null) {
				HashMap<String, String> childMap = new HashMap<>();
				parse(item.getChildNodes(), childMap, results);
				results.add(childMap);
			} else {
				parse(item.getChildNodes(), map, results);
			}
		}
		return results;
	}

	private void addToMap(HashMap<String, String> map, String key, String value) {
		if (map.containsKey(key)) {
			map.put(key, map.get(key) + ", " + value);
		} else {
			map.put(key, value);
		}
	}

	public String getName(Node item) {
		return item.getParentNode() == null ? "" : getName(item.getParentNode()) + "." + item.getNodeName();
	}

}
