package gate.corpora.export;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/*
 * Copyright (c) 1995-2014, The University of Sheffield. See the file
 * COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free software,
 * licenced under the GNU Library General Public License, Version 2, June 1991
 * (in the distribution as file licence.html, and also available at
 * http://gate.ac.uk/gate/licence.html).
 *
 * Mark A. Greenwood 17/07/2014
 *
 */

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.CorpusExporter;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.GateConstants;
import gate.Utils;
import gate.corpora.RepositioningInfo;
import gate.creole.metadata.AutoInstance;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;

@CreoleResource(name = "Twitter JSON Exporter", comment = "Export documents and corpora in Twitter JSON format", tool = true, autoinstances = @AutoInstance, icon = "TwitterJSON")
public class TwitterJsonExporter extends CorpusExporter {

  private static final long serialVersionUID = -8087536348560365618L;

  protected static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * No-op, exists only as a host for the parameter annotations.
   */
  @Optional
  @RunTime
  @CreoleParameter(comment = "The annotation set from which "
      + "otherwise-unspecified entity annotations will be taken")
  public void setEntitiesAnnotationSetName(String name) {
  }

  public String getEntitiesAnnotationSetName() {
    return null;
  }

  /**
   * No-op, exists only as a host for the parameter annotations.
   */
  @RunTime
  @CreoleParameter(comment = "Annotation types to export.  "
      + "Plain annotation types will be taken from the set named "
      + "by the annotationSetName parameter, entries containing "
      + "a colon are treated as setName:type (with an empty setName "
      + "denoting the default set).")
  public void setAnnotationTypes(Set<String> types) {
  }

  public Set<String> getAnnotationTypes() {
    return null;
  }

  /**
   * No-op, exists only as a host for the parameter annotations.
   */
  @RunTime
  @CreoleParameter(defaultValue = "false", comment = "Whether "
      + "to wrap the output as a JSON array.  When exporting a corpus, "
      + "true will write a JSON array of objects, one per document, "
      + "whereas false will simply output one object per document "
      + "separated by newlines.")
  public void setExportAsArray(Boolean array) {
  }

  public Boolean getExportAsArray() {
    return Boolean.FALSE;
  }

  public TwitterJsonExporter() {
    super("Twitter JSON", "json", "text/x-json-twitter");
  }

  @Override
  public void export(Document doc, OutputStream out, FeatureMap options)
      throws IOException {
    try (JsonGenerator generator = openGenerator(out, options)) {
      export(doc, generator, options);
    }
  }

  public void export(Corpus corpus, OutputStream out, FeatureMap options)
      throws IOException {
    try (JsonGenerator generator = openGenerator(out, options)) {
      Iterator<Document> docIter = corpus.iterator();
      int currentDocIndex = 0;
      while(docIter.hasNext()) {
        boolean docWasLoaded = corpus.isDocumentLoaded(currentDocIndex);
        Document currentDoc = docIter.next();
        try {
          export(currentDoc, generator, options);
        } finally {
          // unload if necessary
          if(!docWasLoaded) {
            corpus.unloadDocument(currentDoc);
            Factory.deleteResource(currentDoc);
          }
          currentDocIndex++;
        }
      }
    }
  }

  /**
   * Create a JsonGenerator ready to write to the given output stream. If the
   * specified options indicate that we want to wrap the output in an array then
   * output the array start event in preparation.
   */
  protected JsonGenerator openGenerator(OutputStream out, FeatureMap options)
      throws IOException {
    JsonGenerator generator = MAPPER.getFactory()
        .createGenerator(new OutputStreamWriter(out, "UTF-8"));
    generator.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    generator.enable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT);
    if(options.containsKey("exportAsArray")
        && ((Boolean)options.get("exportAsArray")).booleanValue()) {
      generator.writeStartArray();
    } else {
      // writing concatenated, put newlines in between
      generator.setRootValueSeparator(new SerializedString("\n"));
    }

    return generator;
  }

  public void export(Document doc, JsonGenerator generator, FeatureMap options)
      throws IOException {
    try {
      AnnotationSet defaultEntitiesAS =
          doc.getAnnotations((String)options.get("entitiesAnnotationSetName"));

      @SuppressWarnings("unchecked")
      Collection<String> types =
          (Collection<String>)options.get("annotationTypes");

      Map<String, AnnotationSet> annotationsMap =
          new LinkedHashMap<>();

      if(types != null) {
        for(String type : types) {
          String[] setAndType = type.split(":", 2);
          if(setAndType.length == 1) {
            annotationsMap.put(type, defaultEntitiesAS.get(type));
          } else {
            annotationsMap.put(type,
                doc.getAnnotations(setAndType[0]).get(setAndType[1]));
          }
        }
      }

      List<Annotation> tweets = Utils.inDocumentOrder(
          doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME)
              .get("Tweet"));

      ObjectWriter writer = MAPPER.writer();

      for(Annotation tweet : tweets) {
        FeatureMap features = Factory.newFeatureMap();
        features.putAll(tweet.getFeatures());

        AnnotationSet segments = Utils.getContainedAnnotations(
            doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME),
            tweet, "TweetSegment");

        for(Annotation segment : segments) {
          long start = segment.getStartNode().getOffset();
          String textPath = (String)segment.getFeatures().get("textPath");
          RepositioningInfo repos = new RepositioningInfo();
          String text = escape(Utils.stringFor(doc, segment), repos);
          addToFeatures(features, textPath, text);
          
          // TODO update the features with the entities
          String entitiesPath =
              (String)segment.getFeatures().get("entitiesPath");
          
          FeatureMap annotations = Factory.newFeatureMap();
          
          //filter the annotations map using the segment as the covering annotation
          for (Map.Entry<String, AnnotationSet> entry : annotationsMap.entrySet()) {
            String annotationType = entry.getKey();
            
            List<FeatureMap> annotationsofType = new ArrayList<FeatureMap>();
            
            List<Annotation> segmentAnnotations = Utils.inDocumentOrder(
                Utils.getContainedAnnotations(entry.getValue(), segment));
            
            for (Annotation annotation : segmentAnnotations) {
              FeatureMap representation = Factory.newFeatureMap();
              representation.putAll(annotation.getFeatures());
              
              List<Number> indices = new ArrayList<Number>();
              
              indices.add(repos.getOriginalPos(annotation.getStartNode().getOffset() - start, true));
              indices.add(repos.getOriginalPos(annotation.getEndNode().getOffset() - start, false));
              
              representation.put("indices", indices);
              
              annotationsofType.add(representation);
            }
            
            annotations.put(annotationType,annotationsofType);
          }
          
          addToFeatures(features, entitiesPath, annotations);

        }

        generator.writeStartObject();

        for(Map.Entry<Object, Object> entry : features.entrySet()) {
          generator.writeFieldName(entry.getKey().toString());
          writer.writeValue(generator, entry.getValue());
        }

        generator.writeEndObject();

      }

      generator.flush();

      /*
       * if(docAnnots == null || docAnnots.isEmpty()) { // no document
       * annotations, write everything Map<String, Collection<Annotation>>
       * sortedAnnots = new LinkedHashMap<>(); for(Map.Entry<String,
       * Collection<Annotation>> entry : annotationsMap.entrySet()) {
       * sortedAnnots.put(entry.getKey(),
       * Utils.inDocumentOrder((AnnotationSet)entry.getValue())); }
       * DocumentJsonUtils.writeDocument(doc, 0L, Utils.end(doc), sortedAnnots,
       * null, null, generator); } else { for(Annotation docAnnot :
       * Utils.inDocumentOrder(docAnnots)) { Map<String, Collection<Annotation>>
       * coveredAnnotations = new HashMap<>(); for(Map.Entry<String,
       * Collection<Annotation>> entry : annotationsMap.entrySet()) {
       * coveredAnnotations.put(entry.getKey(),
       * Utils.inDocumentOrder(((AnnotationSet)entry.getValue()).getContained(
       * Utils.start(docAnnot), Utils.end(docAnnot)))); }
       * DocumentJsonUtils.writeDocument(doc, Utils.start(docAnnot),
       * Utils.end(docAnnot), coveredAnnotations, docAnnot.getFeatures(), null,
       * generator); } }
       */

    } finally {
    } /*
       * catch(InvalidOffsetException e) { // should never happen, as all
       * offsets come from the document itself throw new
       * GateRuntimeException("Invalid offset found within document", e); }
       */
  }
  
  private static void addToFeatures(FeatureMap features, String path, Object value) {
    String[] parts = path.split("\\.",2);
    
      if (parts.length == 1) {
        features.put(path, value);
      }
      else {
        if (!features.containsKey(parts[0])) {
          features.put(parts[0], Factory.newFeatureMap());
        }
      
        FeatureMap nested = (FeatureMap)features.get(parts[0]);
        addToFeatures(nested, parts[1], value);
      }
  }
  
  /**
   * Characters to account for when escaping - ampersand, angle brackets, and supplementaries
   */
  private static final Pattern CHARS_TO_ESCAPE = Pattern.compile("[<>&\\x{" +
          Integer.toHexString(Character.MIN_SUPPLEMENTARY_CODE_POINT)+ "}-\\x{" +
          Integer.toHexString(Character.MAX_CODE_POINT) + "}]");
  
  
  /**
   * Escape all angle brackets and ampersands in the given string,
   * recording the adjustments to character offsets within the
   * given {@link RepositioningInfo}.  Also record supplementary
   * characters (above U+FFFF), which count as two in terms of
   * GATE annotation offsets (which count in Java chars) but one
   * in terms of JSON (counting in Unicode characters).
   */
  private static String escape(String str, RepositioningInfo repos) {
    StringBuffer buf = new StringBuffer();
    int origOffset = 0;
    int extractedOffset = 0;
    Matcher mat = CHARS_TO_ESCAPE.matcher(str);
    while(mat.find()) {
      if(mat.start() != extractedOffset) {
        // repositioning record for the span from end of previous match to start of this one
        int nonMatchLen = mat.start() - extractedOffset;
        repos.addPositionInfo(origOffset, nonMatchLen, extractedOffset, nonMatchLen);
        origOffset += nonMatchLen;
        extractedOffset += nonMatchLen;
      }

      // the extracted length is the number of code units matched by the pattern
      int extractedLen = mat.end() - mat.start();
      int origLen = 0;
      String replace = "?";
      switch(mat.group()) {
        case "&":
          replace = "&amp;";
          origLen = 5;
          break;
        case ">":
          replace = "&gt;";
          origLen = 4;
          break;
        case "<":
          replace = "&lt;";
          origLen = 4;
          break;
        default:
          // supplementary character, so no escaping but need to account for
          // it in repositioning info
          replace = mat.group();
          origLen = 1;
      }
      // repositioning record covering this match
      repos.addPositionInfo(origOffset, origLen, extractedOffset, extractedLen);
      mat.appendReplacement(buf, replace);
      origOffset += origLen;
      extractedOffset += extractedLen;

    }
    int tailLen = str.length() - extractedOffset;
    if(tailLen > 0) {
      // repositioning record covering everything after the last match
      repos.addPositionInfo(origOffset, tailLen + 1, extractedOffset, tailLen + 1);
    }
    mat.appendTail(buf);
    return buf.toString();
  }

}
