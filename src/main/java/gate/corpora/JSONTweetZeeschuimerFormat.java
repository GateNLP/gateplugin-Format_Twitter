package gate.corpora;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gate.DocumentContent;
import gate.Resource;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.AutoInstance;
import gate.creole.metadata.CreoleResource;
import gate.util.DocumentFormatException;
import gate.util.InvalidOffsetException;

@CreoleResource(name = "Twitter Zeeschuimer JSON Document Format", isPrivate = true, autoinstances = {
      @AutoInstance(hidden = true) }, comment = "Format parser for Twitter JSON files collected by Zeeschuimer", helpURL = "http://gate.ac.uk/userguide/sec:social:twitter:format")
public class JSONTweetZeeschuimerFormat extends JSONTweetFormat {

   private static final long serialVersionUID = -1864294240674876322L;

   private ObjectMapper objectMapper = new ObjectMapper();

   /** Default construction */
   public JSONTweetZeeschuimerFormat() {
      super();
   }

   /** Initialise this resource, and return it. */
   public Resource init() throws ResourceInstantiationException {
      // Register ad hoc MIME-type
      MimeType mime = new MimeType("text", "x-json-twitter-zeeschuimer");

      // Register the class handler for this MIME-type
      mimeString2ClassHandlerMap.put(mime.getType() + "/" + mime.getSubtype(), this);

      // Register the mime type with string
      mimeString2mimeTypeMap.put(mime.getType() + "/" + mime.getSubtype(), mime);

      // Register file suffixes for this mime type
      suffixes2mimeTypeMap.put("ndjson", mime);

      // Set the mimeType for this language resource
      setMimeType(mime);

      return this;
   }

   @Override
   public void unpackMarkup(gate.Document doc) throws DocumentFormatException {
      if ((doc == null) || (doc.getSourceUrl() == null && doc.getContent() == null)) {
         throw new DocumentFormatException("GATE document is null or no content found. Nothing to parse!");
      }

      setNewLineProperty(doc);
      String jsonString = StringUtils.trimToEmpty(doc.getContent().toString());

      StringBuilder concatenation = new StringBuilder();

      try {
         // now convert the raw response into a proper JSON structure we can process
         JsonNode root = objectMapper.readTree(jsonString);

         ObjectNode tweet = (ObjectNode)root.at("/data");
         
         if (tweet.isMissingNode()) {
            throw new DocumentFormatException("/data is missing so this can't be a Twitter Zeeschuimer JSON file");
         }
         
         tweet = convert(tweet);
         
         tweet.put("platform", "Twitter");

         concatenation.append(objectMapper.writeValueAsString(tweet));
         
      } catch (Exception e) {
         // should only happen if there is an error reading the JSON
         // i.e. it's not actually well formed JSON. In other words never
         throw new DocumentFormatException(e);
      }

      // Set new document content
      DocumentContent newContent = new DocumentContentImpl(concatenation.toString());
      try {
         doc.edit(0L, doc.getContent().size(), newContent);
      } catch (InvalidOffsetException e) {
         // this should be impossible but......
         throw new DocumentFormatException(e);
      }

      super.unpackMarkup(doc);
   }
   
   private ObjectNode convert(ObjectNode data) {
      ObjectNode tweet = (ObjectNode)data.at("/legacy"); 
      
      ObjectNode retweetedStatus = (ObjectNode)tweet.remove("retweeted_status_result");      
      if (retweetedStatus != null) {
         retweetedStatus = (ObjectNode)retweetedStatus.at("/result");
         tweet.set("retweeted_status", convert(retweetedStatus));
      }
      
      ObjectNode quotedStatus = (ObjectNode)data.remove("quoted_status_result");
      
      if (quotedStatus != null) {
         quotedStatus = (ObjectNode)quotedStatus.at("/result");
         tweet.set("quoted_status", convert(quotedStatus));
      }
      
      tweet.set("user", data.at("/core/user_results/result/legacy"));
      
      return tweet;
   }

}
