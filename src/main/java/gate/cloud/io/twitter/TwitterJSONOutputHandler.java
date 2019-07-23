package gate.cloud.io.twitter;

import static gate.cloud.io.IOConstants.PARAM_FILE_EXTENSION;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.GateConstants;
import gate.Utils;
import gate.annotation.AnnotationSetImpl;
import gate.cloud.batch.DocumentID;
import gate.cloud.io.file.AbstractFileOutputHandler;
import gate.corpora.DocumentJsonUtils;
import gate.corpora.export.TwitterJsonExporter;
import gate.util.GateException;
import gate.util.OffsetComparator;

public class TwitterJSONOutputHandler extends AbstractFileOutputHandler {

	private TwitterJsonExporter exporter = new TwitterJsonExporter();

	private static final JsonFactory JSON_FACTORY = new JsonFactory().enable(Feature.AUTO_CLOSE_TARGET);

	@Override
	protected void configImpl(Map<String, String> configData) throws IOException, GateException {
		// make sure we default to .json as the extension
		if (!configData.containsKey(PARAM_FILE_EXTENSION)) {
			configData.put(PARAM_FILE_EXTENSION, ".json");
		}

		super.configImpl(configData);
	}

	@Override
	protected void outputDocumentImpl(Document document, DocumentID documentId) throws IOException, GateException {

		Map<String, Collection<Annotation>> annotationSetsMap = collectAnnotations(document);

		Map<String, Collection<Annotation>> originalMap = annotationSetsMap;
		annotationSetsMap = new HashMap<String, Collection<Annotation>>();
		for (Collection<Annotation> annSet : originalMap.values()) {
			for (Annotation a : annSet) {
				Collection<Annotation> annsByType = annotationSetsMap.get(a.getType());
				if (annsByType == null) {
					annsByType = new AnnotationSetImpl(document);
					annotationSetsMap.put(a.getType(), annsByType);
				}
				annsByType.add(a);
			}
		}

		AnnotationSet originalMarkups = document.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

		OutputStream outputStream = getFileOutputStream(documentId);
		OutputStreamWriter writer = new OutputStreamWriter(outputStream,
				(encoding == null || encoding.length() == 0 ? "UTF-8" : encoding));
		JsonGenerator generator = JSON_FACTORY.createGenerator(writer);

		try {

			if (originalMarkups.get("TweetSegment").isEmpty()) {
				Comparator<Annotation> comparator = new OffsetComparator();
				for (Map.Entry<String,Collection<Annotation>> entry : annotationSetsMap.entrySet()) {
					List<Annotation> list = new ArrayList<Annotation>();
					list.addAll(entry.getValue());
					Collections.sort(list, comparator);
					entry.setValue(list);
				}
				
				AnnotationSet documentAnnotationSet = originalMarkups.get("Tweet");
				if (documentAnnotationSet.size() > 1) {
					throw new GateException("Found more than one Tweet annotation for document " + documentId);
				}
				if (documentAnnotationSet.size() > 0) {
					Annotation documentAnnotation = Utils.getOnlyAnn(documentAnnotationSet);
					DocumentJsonUtils.writeDocument(document, Utils.start(documentAnnotation),
							Utils.end(documentAnnotation), annotationSetsMap, documentAnnotation.getFeatures(), null,
							generator);
					return;
				}

				// if we get here we either didn't have documentAnnotationType
				// set, or it was set but the document contained no such
				// annotation - simply output the whole document with no extra
				// features.
				DocumentJsonUtils.writeDocument(document, 0L, Utils.end(document), annotationSetsMap, null, null,
						generator);

			} else {
				// we have an actual tweet object we can do something with

				// create a FeatureMap of options to drive the underlying Twitter exporter
				FeatureMap options = Factory.newFeatureMap();
				options.put("annotationsMap", annotationSetsMap);

				// export the document
				exporter.export(document, generator, options);
			}

		} finally {
			generator.close();
		}
	}
}
