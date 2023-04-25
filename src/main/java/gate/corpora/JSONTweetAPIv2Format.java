package gate.corpora;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gate.DocumentContent;
import gate.Resource;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.AutoInstance;
import gate.creole.metadata.CreoleResource;
import gate.util.DocumentFormatException;
import gate.util.InvalidOffsetException;

@CreoleResource(name = "Twitter v2 JSON Document Format", isPrivate = true, autoinstances = {
      @AutoInstance(hidden = true) }, comment = "Format parser for Twitter v2 JSON files", helpURL = "http://gate.ac.uk/userguide/sec:social:twitter:format")
public class JSONTweetAPIv2Format extends JSONTweetFormat {

   private static final long serialVersionUID = -1857687524024874114L;

   // this allows us to parse the dates coming back from the v2 API
   private static final DateTimeFormatter NEW_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
         .withZone(ZoneId.of("Z")).withLocale(Locale.ENGLISH);

   // this allows us to format dates in the same way as the old v1.1 API
   private static final DateTimeFormatter OLD_DATE_FORMAT = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss Z yyyy")
         .withZone(ZoneId.of("Z")).withLocale(Locale.ENGLISH);

   private ObjectMapper objectMapper = new ObjectMapper();

   /** Default construction */
   public JSONTweetAPIv2Format() {
      super();
   }

   /** Initialise this resource, and return it. */
   public Resource init() throws ResourceInstantiationException {
      // Register ad hoc MIME-type
      MimeType mime = new MimeType("text", "x-json-twitter-v2");

      // Register the class handler for this MIME-type
      mimeString2ClassHandlerMap.put(mime.getType() + "/" + mime.getSubtype(), this);

      // Register the mime type with string
      mimeString2mimeTypeMap.put(mime.getType() + "/" + mime.getSubtype(), mime);

      // Register file suffixes for this mime type
      suffixes2mimeTypeMap.put("json", mime);

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

         if (root.at("/data").isMissingNode()) {
            throw new DocumentFormatException("/data is missing so this can't be a Twitter v2 JSON file");
         }

         // having got this far there must be data to actually process so.....

         // maps to hold all the referenced users and tweets as we process them
         Map<String, ObjectNode> userObjectsByID = new HashMap<String, ObjectNode>();
         Map<String, ObjectNode> userObjectsByName = new HashMap<String, ObjectNode>();
         Map<String, ObjectNode> tweetObjects = new HashMap<String, ObjectNode>();

         /**
          * STEP 1: Process the /includes/users and store in a map keyed on screen name
          **/

         // the info on users is all in /includes/users so let's get that and work
         // through it
         ArrayNode userNodes = (ArrayNode) root.at("/includes/users");

         for (int i = 0; i < userNodes.size(); ++i) {
            // for each of the users....
            ObjectNode user = (ObjectNode) userNodes.get(i);

            // move the username and id into screen_name and id_str to match v1.1
            user.put("screen_name", user.remove("username"));
            user.put("id_str", user.remove("id"));

            // move the user profile into the right place
            user.put("profile_image_url_https", user.remove("profile_image_url"));

            // convert the new style date format into the v1.1 format
            ZonedDateTime date = ZonedDateTime.parse(user.remove("created_at").asText(), NEW_DATE_FORMAT);
            user.put("created_at", date.format(OLD_DATE_FORMAT));

            // the network info for a user is in public_metrics pull this out and put it
            // at the top level under the original v1.1 fields
            ObjectNode metrics = (ObjectNode) user.remove("public_metrics");
            user.put("followers_count", metrics.at("/followers_count"));
            user.put("friends_count", metrics.at("/following_count"));
            user.put("listed_count", metrics.at("/listed_count"));
            user.put("statuses_count", metrics.at("/tweet_count"));

            // now store the v1.1 user object in the map ready for when we need it
            userObjectsByID.put(user.at("/id_str").asText(), user);
            userObjectsByName.put(user.at("/screen_name").asText(), user);
         }

         /** STEP 2: Process the /includes/tweets and store in a map keyed on id **/

         // referenced tweets (retweets and quote tweets etc.) are at /includes/tweets
         JsonNode tweetNodes = root.at("/includes/tweets");

         if (!tweetNodes.isMissingNode()) {
            // assuming there are some....

            for (int i = 0; i < tweetNodes.size(); ++i) {
               // get the tweet
               ObjectNode tweet = (ObjectNode) tweetNodes.get(i);

               // convert it to the v1.1 format
               convertTweet(tweet, userObjectsByID, userObjectsByName);

               // store it in the map ready for when we need it
               tweetObjects.put(tweet.at("/id_str").asText(), tweet);
            }
         }

         // now we've converted each referenced tweet we need to process them to
         // so that any tweet they reference is correctly embeded within it
         // for (Map.Entry<String,ObjectNode> entry : tweetObjects.entrySet()) {
         // processReferencedTweets(entry.getValue(),tweetObjects);
         // }

         // how many objects and referenced tweets have we found?
         System.err.println("Included Objects:");
         System.err.println("\tusers: " + userObjectsByName.size());
         System.err.println("\ttweets: " + tweetObjects.size());

         /**
          * STEP 3: now that we've dealt with the referenced users and tweets we can
          * finally
          **/
         /** work through the actual tweets that matched the query and convert them **/

         // so get the matching tweets
         tweetNodes = root.at("/data");

         if (tweetNodes.isArray()) {
            for (int i = 0; i < tweetNodes.size(); ++i) {
               // for each tweet
               ObjectNode tweet = (ObjectNode) tweetNodes.get(i);

               // convert it to v1.1
               convertTweet(tweet, userObjectsByID, userObjectsByName);

               // expand any referenced tweets
               processReferencedTweets(tweet, tweetObjects);

               // write the final v1.1 JSON out to the file
               // objectMapper.writeTree(oldOut, tweet)
               concatenation.append(objectMapper.writeValueAsString(tweet));
            }
         } else if (tweetNodes.isObject()) {
            ObjectNode tweet = (ObjectNode)tweetNodes;

            // convert it to v1.1
            convertTweet(tweet, userObjectsByID, userObjectsByName);

            // expand any referenced tweets
            processReferencedTweets(tweet, tweetObjects);

            // write the final v1.1 JSON out to the file
            // objectMapper.writeTree(oldOut, tweet)
            concatenation.append(objectMapper.writeValueAsString(tweet));
         } else {
            // err... what now?
            throw new DocumentFormatException("the /data element is neither an object or an array");
         }
      } catch (Exception e) {
         // should only happen if there is an error reading the v2 JSON
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

   /**
    * Rearranges an ObjectNode representing a tweet to take it from the v2 API
    * format back to something that matches (as close as possible) the v1.1 API
    **/
   public void convertTweet(ObjectNode tweet, Map<String, ObjectNode> userObjectsByID,
         Map<String, ObjectNode> userObjectsByName) {
      // move the id into the correctly named field for v1.1
      tweet.put("id_str", tweet.remove("id"));

      // remove the ID of the user being replied to as it's always in the wrong place
      JsonNode node = tweet.remove("in_reply_to_user_id");

      if (node != null) {
         // if this is a reply to a user then...

         // stick the ID in the right field
         tweet.put("in_reply_to_user_id_str", node);

         // get the user object we built before from the map
         ObjectNode user = userObjectsByID.get(node.asText());

         // and put the screen_name into the right place or log a message
         // if we don't have the info (probably as the account has been deleted)
         if (user == null)
            System.err.println("missing user " + node.asText());
         else {
            tweet.put("in_reply_to_screen_name", user.get("screen_name"));
         }
      }

      // get the user object we built for this tweet author and stick it
      // in the v1.1 user field or log an error if it's missing
      // TODO we aren't we removing author_id at this point?
      ObjectNode author = userObjectsByID.get(tweet.at("/author_id").asText());
      if (author == null)
         System.err.println("missing author " + tweet.at("/author_id").asText());
      else {
         tweet.put("user", author);
      }

      // convert the new date format back to the old v1.1 version
      ZonedDateTime date = ZonedDateTime.parse(tweet.remove("created_at").asText(), NEW_DATE_FORMAT);
      tweet.put("created_at", date.format(OLD_DATE_FORMAT));

      // move the public metrics onto the top level object
      ObjectNode metrics = (ObjectNode) tweet.remove("public_metrics");
      tweet.put("retweet_count", metrics.at("/retweet_count"));
      tweet.put("reply_count", metrics.at("/reply_count"));
      tweet.put("favorite_count", metrics.at("/like_count"));
      tweet.put("quote_count", metrics.at("/quote_count"));

      // TODO add the boolean fields we can derive from the public_metrics

      // now we need to process the entities if it has any...
      ObjectNode entities = (ObjectNode) tweet.get("entities");
      if (entities != null) {

         // currently the offsets for annotations are broken so we remove them
         // entirely otherwise we can't load the JSON into GATE
         entities.remove("annotations");

         // mentions should be user_mentions
         node = entities.remove("mentions");
         if (node != null)
            entities.put("user_mentions", node);

         // cashtags should be symbols
         node = entities.remove("cashtags");
         if (node != null)
            entities.put("symbols", node);

         // for (String type : entities.fieldNames()) {
         Iterator<String> it = entities.fieldNames();
         while (it.hasNext()) {
            String type = it.next();

            // for each type of entity get all the instances
            ArrayNode instances = (ArrayNode) entities.get(type);

            for (int i = 0; i < instances.size(); ++i) {
               // and for each instance
               ObjectNode instance = (ObjectNode) instances.get(i);

               // create an array to hold the offsets
               ArrayNode indices = objectMapper.createArrayNode();

               // move the offsets into the array
               indices.add(instance.remove("start"));
               indices.add(instance.remove("end"));

               // then store the array back into the instance
               instance.put("indices", indices);

               if ("hashtags".equals(type) || "symbols".equals(type)) {
                  // hahstags and symbols should have a text feature but it's now tag so...
                  instance.put("text", instance.remove("tag"));
               } else if ("user_mentions".equals(type)) {
                  // user mentions need the username moving to screen_name and...
                  instance.put("screen_name", instance.remove("username"));

                  ObjectNode userNode = userObjectsByName.get(instance.get("screen_name"));
                  if (userNode != null) {
                     // ... the other user fields adding if we know them
                     instance.setAll(userNode);
                  }
               }
            }
         }
      }
   }

   /**
    * A tweet can reference other tweets, such as retweet or quote tweet. In the
    * new API these are just IDs whereas in v1.1 they are full JSON objects so...
    * we need to expand the tweet to include the referenced tweets where possible.
    * Currently we can only do this one layer deep as we only get expanded objects
    * for those tweets matching the query and not all the tweets they reference. If
    * we want to follow the rabbit hole as deep as it goes then we'd need to
    * essentially spawn a bunch more requests to get individual tweets.
    **/
   protected static void processReferencedTweets(ObjectNode tweet, Map<String, ObjectNode> tweetObjects) {
      // get the array of reference tweets. although it's not in the v1.1 object model
      // for now we are leaving the data in place so that we can spot holes in the
      // expanded
      // data due to the issue of only getting info one level deep etc.
      ArrayNode referencedTweets = (ArrayNode) tweet.get("referenced_tweets");

      if (referencedTweets != null) {
         for (int j = 0; j < referencedTweets.size(); ++j) {
            // for each tweet referenced...
            ObjectNode referencedTweet = (ObjectNode) referencedTweets.get(j);

            // get the tpe of the reference and the ID of the tweet
            String type = referencedTweet.get("type").asText();
            String id = referencedTweet.get("id").asText();

            // see if we have the tweet in those we've already expanded
            ObjectNode rTweet = tweetObjects.get(id);

            if ("replied_to".equals(type)) {
               // this is a reply so we just need to fill in the right field with the ID
               tweet.put("in_reply_to_status_id_str", id);

               // TODO should we put the original in a field like retweet_status even though
               // that wouldn't confirm strictly to the v1.1 object model
               // if (rTweet != null) tweet.put("replied_to_status",rTweet);
            } else if ("retweeted".equals(type)) {
               // it's a retweet so if we have the tweet object then put it in the right place
               if (rTweet != null)
                  tweet.put("retweeted_status", rTweet);
            } else if ("quoted".equals(type)) {
               // we are quoting a tweet so stick the id in one field and...
               tweet.put("quoted_status_id_str", id);

               // if we have the full tweet put that in the other field
               if (rTweet != null)
                  tweet.put("quoted_status", rTweet);

               // TODO boolean is_quote_status field, should be false except for these
            } else {
               // as far as I know this should never happen!
               System.err.println("unknown referenced tweet type: " + type);
            }
         }
      }
   }
}
