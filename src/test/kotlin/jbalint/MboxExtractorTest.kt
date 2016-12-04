package jbalint

import com.complexible.common.openrdf.model.ModelIO
import com.complexible.common.openrdf.model.Models2
import com.complexible.common.rdf.model.StardogValueFactory
import com.complexible.stardog.db.DatabaseConnection
import com.complexible.stardog.docs.StardocsVocabulary
import com.google.common.collect.Lists
import org.junit.Assert
import org.junit.Test
import org.openrdf.model.util.Models
import org.openrdf.model.vocabulary.RDF
import org.openrdf.rio.RDFFormat
import java.io.FileWriter
import java.nio.file.Paths

/**
 * Tests for the `mbox' extractor
 */
class MboxExtractorTest {
    val RESOURCES_PATH = "src/test/resources/"

    fun verifyExtraction(mboxFile: String, expectedRdfModelFile: String) {
        val extractor = MboxExtractor()
        val dbConn: DatabaseConnection? = null
        val testInput = Paths.get(RESOURCES_PATH + mboxFile)
        val statementSource = extractor.extract(dbConn, StardocsVocabulary.ontology().term("test.mbox"), testInput)
//        ModelIO.write(Models2.newModel(statementSource.statements()), FileWriter("/tmp/z1.ttl"), RDFFormat.N3)
        val expected = ModelIO.read(Paths.get(RESOURCES_PATH + expectedRdfModelFile))
        val actual = Lists.newArrayList(statementSource.statements())
        Assert.assertTrue(Models.isomorphic(expected, actual))
    }

    @Test
    fun singleMessage() {
        verifyExtraction("stardog_42_release.mbox", "stardog_42_release_expected.ttl")
    }

    @Test
    fun collection() {
        verifyExtraction("lucene_collection.mbox", "lucene_collection_expected.ttl")
    }

    @Test
    fun badOutput() {
        val vf = StardogValueFactory.instance()
        ModelIO.write(Models2.newModel(vf.createStatement(RDF.SUBJECT, RDF.TYPE, vf.createIRI("http://somePrefix/", "asdf> <jkl"))), FileWriter("/tmp/bad_output.ttl"), RDFFormat.N3)
    }
}