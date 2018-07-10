/*
 *  Copyright (c) 1995-2014, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *  
 *  $Id: Tweet.java 19779 2016-11-24 10:18:32Z markagreenwood $
 */
package gate.corpora.twitter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import gate.Factory;
import gate.FeatureMap;
import gate.corpora.RepositioningInfo;
import gate.util.Strings;


public class Tweet {
  private StringBuilder string;
  private Set<PreAnnotation> annotations;
  
  
  public Set<PreAnnotation> getAnnotations() {
    return this.annotations;
  }
  
  public int getLength() {
    return this.string.length();
  }

  public String getString() {
    return this.string.toString();
  }

  /**
   * Used by the JSONTWeetFormat; the DocumentContent contains only the main text;
   * the annotation feature map contains all the other JSON data, recursively.
   */
  public Tweet(JsonNode json, boolean handleEntities) {
    string = new StringBuilder();
    Iterator<String> keys = json.fieldNames();
    FeatureMap features = Factory.newFeatureMap();
    annotations = new HashSet<PreAnnotation>();

    while (keys.hasNext()) {
      String key = keys.next();
      features.put(key, TweetUtils.process(json.get(key)));
    }
    
    unpackTweets(features,null,"tweet");
    
    /*while (keys.hasNext()) {
      String key = keys.next();

      if (key.equals(TweetUtils.DEFAULT_TEXT_ATTRIBUTE)) {
        RepositioningInfo repos = new RepositioningInfo();
        string = unescape(json.get(key).asText(), repos);
        if(handleEntities) processEntities(json, 0L, repos);
      } else if(key.equals("entities") && handleEntities) {
        // do nothing - don't add entities as a feature
      } else {
        features.put(key, TweetUtils.process(json.get(key)));
      }
    }*/
    
    annotations.add(new PreAnnotation(0L, string.length(), TweetUtils.TWEET_ANNOTATION_TYPE, features));
    
  }
  
  private void unpackTweets(FeatureMap features, String path, String type) {
    String expandedPath = path == null ? "" : path+".";
    if (features.containsKey("retweeted_status")) {
      unpackTweets((FeatureMap)features.get("retweeted_status"), expandedPath+"retweeted_status", "retweet");
      return;
    }
    
    if (features.containsKey("full_text")) {
      unpackTextAndEntities(features, expandedPath, "full_text", type);      
    } else if (features.containsKey("extended_tweet")) {
      unpackTweets((FeatureMap)features.get("extended_tweet"), expandedPath+"extended_tweet", type);
    } else if (features.containsKey("text")) {
      unpackTextAndEntities(features, expandedPath, "text", type);
    }
    
    if (features.containsKey("quoted_status")) {
      unpackTweets((FeatureMap)features.get("quoted_status"), expandedPath+"quoted_status", "quotedTweet");
    }
  }
  
  private void unpackTextAndEntities(FeatureMap features, String expandedPath, String key, String type) {
    if (string.length() != 0) {
      string.append("\n\n");
    }
    
    String content = features.remove(key).toString();
    boolean hasEntities = features.containsKey(TweetUtils.ENTITIES_ATTRIBUTE);
    
    RepositioningInfo repos = new RepositioningInfo();      
    content = unescape(content, repos);
    
    long start = string.length();
    string.append(content);
    
    FeatureMap segmentFeatures = Factory.newFeatureMap();
    segmentFeatures.put("textPath", expandedPath+key);
    segmentFeatures.put("tweetType", type);
    if (hasEntities) segmentFeatures.put("entitiesPath", expandedPath+TweetUtils.ENTITIES_ATTRIBUTE);
    
    annotations.add(new PreAnnotation(start, string.length(), "TweetSegment", segmentFeatures));
    
    if (!hasEntities) return;
    
    FeatureMap entities = (FeatureMap)features.remove(TweetUtils.ENTITIES_ATTRIBUTE);
    
    for (Map.Entry<Object,Object> entity : entities.entrySet()) {
      String entityType = entity.getKey().toString();
      
      List<FeatureMap> instances = (List<FeatureMap>)entity.getValue();
      
      for (FeatureMap instance : instances) {
        List<Number> position = (List<Number>)instance.remove("indices");
        
        long annStart = start + repos.getExtractedPos(position.get(0).longValue());
        long annEnd = start + repos.getExtractedPos(position.get(1).longValue());
        
        annotations.add(new PreAnnotation(annStart, annEnd, entityType, instance));
      }
      
    }
  }
    
  /**
   * Characters to account for in unescaping - HTML-encoded ampersand and angle
   * brackets, and supplementary characters (which don't need "unescaping" but do
   * need to be accounted for in the repos info).
   */
  private static Pattern UNESCAPE_PATTERN = Pattern.compile("&(?:amp|lt|gt);|[\\x{" +
    Integer.toHexString(Character.MIN_SUPPLEMENTARY_CODE_POINT)+ "}-\\x{" +
    Integer.toHexString(Character.MAX_CODE_POINT) + "}]");
  
  /**
   * Un-escape &amp;amp;, &amp;gt; and &amp;lt; in the given string, populating
   * the supplied {@link RepositioningInfo} to describe the offset changes.  Also
   * record the position of any Unicode supplementary characters, as Twitter's
   * entities format counts in characters (so a supplementary is 1) whereas GATE
   * annotations count in Java <code>char</code> values (UTF-16 code units, so
   * a supplementary counts as two).
   * @param str string, possibly including escaped ampersands or angle brackets
   * @param repos {@link RepositioningInfo} to hold offset changes
   * @return the unescaped string
   */
  private String unescape(String str, RepositioningInfo repos) {
    StringBuffer buf = new StringBuffer();
    int origOffset = 0;
    int extractedOffset = 0;
    Matcher mat = UNESCAPE_PATTERN.matcher(str);
    while(mat.find()) {
      if(mat.start() != origOffset) {
        // repositioning record for the span from end of previous match to start of this one
        int nonMatchLen = mat.start() - origOffset;
        repos.addPositionInfo(origOffset, nonMatchLen, extractedOffset, nonMatchLen);
        origOffset += nonMatchLen;
        extractedOffset += nonMatchLen;
      }      
      
      // in most cases the original length is the number of code units the pattern matched
      int origLen = mat.end() - mat.start();
      // and the extracted result is one code unit
      int extractedLen = 1;
      String replace = "?";
      switch(mat.group()) {
        case "&amp;": replace = "&"; break;
        case "&gt;": replace = ">"; break;
        case "&lt;": replace = "<"; break;
        default:
          // but in the case of supplementary characters, the original length
          // (in *characters*) is 1 but the extracted length (in code units) is 2
          replace = mat.group();
          origLen = 1;
          extractedLen = 2;
      }
      mat.appendReplacement(buf, replace);
      // repositioning record covering this match
      repos.addPositionInfo(origOffset, origLen, extractedOffset, extractedLen);

      origOffset += origLen;
      extractedOffset += extractedLen;
    }
    int tailLen = str.length() - origOffset;
    if(tailLen > 0) {
      // repositioning record covering everything after the last match
      repos.addPositionInfo(origOffset, tailLen + 1, extractedOffset, tailLen + 1);
    }
    mat.appendTail(buf);
    return buf.toString();
  }

  /**
   * Process the "entities" property of this json object into annotations,
   * shifting their offsets by the specified amount.
   * 
   * @param json the Tweet json object
   * @param startOffset offset correction if the text is not the first of
   *         the content keys.
   */
  private void processEntities(JsonNode json, long startOffset, RepositioningInfo repos) {
    JsonNode entitiesNode = json.get(TweetUtils.ENTITIES_ATTRIBUTE);
    if(entitiesNode == null || !entitiesNode.isObject()) {
      // no entities, nothing to do
      return;
    }
    Iterator<String> entityTypes = entitiesNode.fieldNames();
    while(entityTypes.hasNext()) {
      String entityType = entityTypes.next();
      JsonNode entitiesOfType = entitiesNode.get(entityType);
      if(entitiesOfType != null && entitiesOfType.isArray() && entitiesOfType.size() > 0) {
        // if the entityType is X:Y then assume X is the AS name and Y is the actual type
        String[] setAndType = entityType.split(":", 2);
        Iterator<JsonNode> it = entitiesOfType.elements();
        while(it.hasNext()) {
          JsonNode entity = it.next();
          if(entity.isObject()) {
            // process is guaranteed to return a FeatureMap given an object
            FeatureMap features = (FeatureMap)TweetUtils.process(entity);
            Object indices = features.get("indices");
            if(indices != null && indices instanceof List<?>) {
              List<?> indicesList = (List<?>)indices;
              if(indicesList.get(0) instanceof Number && indicesList.get(1) instanceof Number) {
                // finally we know we have a valid entity
                features.remove("indices");
                long annStart = repos.getExtractedPos(startOffset + ((Number)indicesList.get(0)).longValue());
                long annEnd = repos.getExtractedPos(startOffset + ((Number)indicesList.get(1)).longValue());
                if(setAndType.length == 2) {
                  // explicit annotation set name
                  annotations.add(new PreAnnotation(annStart, annEnd, setAndType[0], setAndType[1], features));
                } else {
                  annotations.add(new PreAnnotation(annStart, annEnd, entityType, features));
                }
              }
            }
          }
        }
      }
    }
  }
  
}
