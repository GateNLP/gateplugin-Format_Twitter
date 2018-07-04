# Plan for Upgrading to New Style Twitter JSON

## Representation

- When loaded each Tweet is covered by a single `Tweet` annotation, with the features of this annotation being the features pulled from the tweet JSON.
- Each section of text is covered by a `TweetSegment` annotation. This annotation has two features
  - `textPath` the path (dotted sperated feature names) through the features on the `Tweet` annotation to the text element
  - `entitiesPath` the path through the features on the `Tweet` annotation to the element storing the entities

## Parsing

- each Tweet object, regardless of depth, is processed in the following way
  - if it contains a `full_text` element then this is turned into a `TweetSegment`
    - entities are pulled from the `entities` sibling element
  - if it contains a `extended_tweet` this is processed recursively
  - if it contains a `retweeted_status` this is processed recursively
  - if it contains a `quoted_status` this is processed recursively
  - if it contains none of the above but has a `text` element then it's an old style tweet so we use it's `text` and `entities` to create the `TweetSegement` annotation


## Serialization

- we serialize each `Tweet` annotation in turn
  - using any of it's features as the starting point for the JSON output
- we then process each `TweetSegement` in turn
  - putting it's text into the element specified by `textPath`
  - storing any annotations into the element specified by `entitiesPath`
    - annotation offsets have to be adjusted to fit within the `TweetSegment` instead of the document
