package gate.corpora.export;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gate.Document;
import gate.DocumentExporter;
import gate.Factory;
import gate.Utils;
import gate.test.GATEPluginTestCase;

public class TwitterJsonExporterTest extends GATEPluginTestCase {

  ObjectMapper mapper = new ObjectMapper();

  public void testExistingAnnotations() throws Exception {
    // document that has one "examples" annot in the default set within the
    // quoted_status span, plus one already in the matching "entities" map
    Document tweetDoc = Factory.newDocument(this.getClass().getResource("testTweet1.xml"));

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DocumentExporter twitterExporter = DocumentExporter.getInstance("gate.corpora.export.TwitterJsonExporter");
    twitterExporter.export(tweetDoc, baos, Utils.featureMap(
            "entitiesAnnotationSetName", "",
            "annotationTypes", Arrays.asList("examples")));

    JsonNode tweetJson = mapper.readTree(baos.toByteArray());
    // check that the exporter merged the exported annotation into the existing
    // entities map rather than replacing the whole lot
    assertEquals("Expected two \"examples\" entities, but was "
                    + tweetJson.at("/quoted_status/entities/examples"),
            2, tweetJson.at("/quoted_status/entities/examples").size());
  }
}
