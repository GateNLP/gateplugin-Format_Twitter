package gate.cloud.io.twitter;

import static gate.cloud.io.IOConstants.PARAM_FILE_EXTENSION;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.ObjectMapper;

import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.GateConstants;
import gate.cloud.batch.AnnotationSetDefinition;
import gate.cloud.batch.DocumentID;
import gate.cloud.io.OutputHandler;
import gate.cloud.io.file.AbstractFileOutputHandler;
import gate.corpora.export.TwitterJsonExporter;
import gate.util.GateException;

public class TwitterJSONOutputHandler extends AbstractFileOutputHandler implements InvocationHandler {

	private TwitterJsonExporter exporter = new TwitterJsonExporter();

	protected static final ObjectMapper MAPPER = new ObjectMapper();

	private OutputHandler jsonOutputHandler, jsonProxyHandler;

	public TwitterJSONOutputHandler() {
		try {
			jsonOutputHandler = (OutputHandler) (Class.forName("gate.cloud.io.file.JSONOutputHandler").newInstance());
			jsonProxyHandler = (OutputHandler) Proxy.newProxyInstance(this.getClass().getClassLoader(),
					new Class<?>[] { OutputHandler.class }, this);

		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new RuntimeException("Can't find the standard JSON output handler", e);
		}
	}

	@Override
	protected void configImpl(Map<String, String> configData) throws IOException, GateException {
		// make sure we default to .json as the extension
		if (!configData.containsKey(PARAM_FILE_EXTENSION)) {
			configData.put(PARAM_FILE_EXTENSION, ".json");
		}

		super.configImpl(configData);

		Map<String, String> jsonConfig = new HashMap<String, String>(configData);
		jsonConfig.put("groupEntitiesBy", "entities");
		jsonConfig.put("documentAnnotationASName", GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);
		jsonConfig.put("documentAnnotationType", "Tweet");
		jsonProxyHandler.config(jsonConfig);
	}

	@Override
	protected void outputDocumentImpl(Document document, DocumentID documentId) throws IOException, GateException {

		AnnotationSet originalMarkups = document.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

		if (originalMarkups.get("TweetSegment").isEmpty()) {
			// if there are no TweetSegment annotations fall back to the normal JSON handler
			// pre-configured to treat Tweet in Original markups as the document
			jsonProxyHandler.setAnnSetDefinitions(getAnnSetDefinitions());
			jsonProxyHandler.outputDocument(document, documentId);
		} else {
			// we have an actual tweet object we can do something with
			OutputStream outputStream = getFileOutputStream(documentId);
			JsonGenerator generator = MAPPER.getFactory()
					.createGenerator(new OutputStreamWriter(outputStream, "UTF-8"));
			generator.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
			generator.enable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT);
			generator.setRootValueSeparator(new SerializedString("\n"));

			FeatureMap options = Factory.newFeatureMap();

			Set<String> annotationTypes = new HashSet<String>();

			for (AnnotationSetDefinition definition : getAnnSetDefinitions()) {
				for (String type : definition.getAnnotationTypes()) {
					annotationTypes.add(definition.getAnnotationSetName() + ":" + type);
				}
			}

			options.put("annotationTypes", annotationTypes);

			exporter.export(document, generator, options);
		}
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getName().equals("getFileOutputStream"))
			return getFileOutputStream((DocumentID) args[0]);

		return method.invoke(jsonOutputHandler, args);
	}
}
