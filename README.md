mbox-to-rdf
===========

*mbox-to-rdf* is a
Stardog [BITES](http://docs.stardog.com/#_unstructured_data) extractor
to import mbox-format files. The extractor works both with individual
messages and collections such as mailing list archives.

Example
-------

Given the
file
[`stardog_42_release.mbox`](src/test/resources/stardog_42_release.mbox) which
is an mbox-formatted email message, we can load it into Stardog using
BITES:

```shell
$ stardog doc put -r MboxExtractor mbox stardog_42_release.mbox
Successfully put document in the document store: tag:stardog:api:docs:mbox:stardog_42_release.mbox

$ stardog query execute mbox 'select * { graph ?doc { ?message mbox:subject ?subj. FILTER(contains(?subj, "Released")) } }'
stardog query execute mbox 'select * { graph ?doc { ?message mbox:subject ?subj. FILTER(contains(?subj, "Released")) } }'
+---------------------------------------------------+----------------------------------------------------------------------------------+------------------------+
|                        doc                        |                                     message                                      |          subj          |
+---------------------------------------------------+----------------------------------------------------------------------------------+------------------------+
| tag:stardog:api:docs:mbox:stardog_42_release.mbox | tag:stardog:api:mbox:CAKqS+r0pFk1YGqfZHgyDR5kbDnjGbP+HbiCZZ=8mp+C8af4YxA@mail.gm | "Stardog 4.2 Released" |
|                                                   | ail.com                                                                          |                        |
+---------------------------------------------------+----------------------------------------------------------------------------------+------------------------+

Query returned 1 results in 00:00:00.099
```

Installation
------------

The *mbox-to-rdf* extractor can be compiled to a single jar containing
all dependencies (except Stardog) by using the `shadowJar` Gradle
task. This task will create a large jar file as
`build/libs/mbox-to-rdf-0.1-all.jar` where *0.1* is the version
number. Installation only requires copying this jar to your Stardog
installation under `server/dbms/` and restarting the instance.

Once the jar file is deployment, a new extractor called
`MboxExtractor` will be available for use in BITES requests.

Vocabulary
----------

As seen in [`MboxExtractor.kt`](src/main/kotlin/MboxExtractor.kt), the
following classes and properties are used in the generated RDF model
for an email message. The prefix `mbox` is defined as
`tag:stardog:api:mbox:`.

### Classes

| Class          | Description      |
|----------------+------------------|
| `mbox:Message` | An email message |

### Properties

| Property          | Description                                                                                                              |
|-------------------+--------------------------------------------------------------------------------------------------------------------------|
| `mbox:references` | The given message references the pointed-to message                                                                      |
| `mbox:inReplyTo`  | The given message is a reply to the pointed-to-message                                                                   |
| `mbox:from`       | The given message was sent by the entity. The entity is a bnode with a `foaf:mbox` andoptionally a `foaf:name` property. |
| `mbox:to`         | The given message was sent to the entity. The entity is a bnode with a `foaf:mbox` andoptionally a `foaf:name` property. |
| `mbox:subject`    | The subject of the given message.                                                                                        |
| `mbox:date`       | The date the given message was sent.                                                                                     |
| `mbox:body`       | The body text of the given message.                                                                                      |
