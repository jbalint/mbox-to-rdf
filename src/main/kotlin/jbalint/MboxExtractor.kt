package jbalint

import com.complexible.common.rdf.StatementSource
import com.complexible.common.rdf.impl.AbstractStatementSource
import com.complexible.common.rdf.impl.DefaultStatementIterator
import com.complexible.common.rdf.model.StardogValueFactory
import com.complexible.stardog.db.DatabaseConnection
import com.complexible.stardog.docs.extraction.RDFExtractor
import com.google.common.io.CharStreams
import org.apache.james.mime4j.dom.Body
import org.apache.james.mime4j.dom.Message
import org.apache.james.mime4j.dom.Multipart
import org.apache.james.mime4j.dom.TextBody
import org.apache.james.mime4j.dom.address.Mailbox
import org.apache.james.mime4j.dom.field.UnstructuredField
import org.apache.james.mime4j.mboxiterator.MboxIterator
import org.apache.james.mime4j.message.DefaultMessageBuilder
import org.apache.james.mime4j.stream.MimeConfig
import org.openrdf.model.IRI
import org.openrdf.model.Resource
import org.openrdf.model.Statement
import org.openrdf.model.Value
import org.openrdf.model.vocabulary.FOAF
import org.openrdf.model.vocabulary.RDF
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

/**
 * Stardog BITES extractor to build an RDF model from an `mbox' mailbox.
 */
class MboxExtractor : RDFExtractor {
    // value factory to create values and statements
    val VF = StardogValueFactory.instance()!!

    val messageBuilder = DefaultMessageBuilder()

    val IRI_PREFIX = "tag:stardog:api:mbox:"

    val MESSAGE = VF.createIRI(IRI_PREFIX, "Message")!!
    val REFERENCES = VF.createIRI(IRI_PREFIX, "references")!!
    val IN_REPLY_TO = VF.createIRI(IRI_PREFIX, "inReplyTo")!!
    val FROM = VF.createIRI(IRI_PREFIX, "from")!!
    val TO = VF.createIRI(IRI_PREFIX, "to")!!
    val SUBJECT = VF.createIRI(IRI_PREFIX, "subject")!!
    val DATE = VF.createIRI(IRI_PREFIX, "date")!!
    val BODY = VF.createIRI(IRI_PREFIX, "body")!!

    // remove the leading and trailing "<" ">"
    val strip = { x: String -> x.drop(1).dropLast(1) }

    init {
        messageBuilder.setMimeEntityConfig(MimeConfig.Builder().setMaxLineLen(10000).build())
    }

    fun extractBody(body: Body): String? {
        if (body is TextBody) {
            return CharStreams.toString(body.reader)
        } else if (body is Multipart) {
            return body.bodyParts.map { extractBody(it.body) }.joinToString("\n")
        } else {
            return null
        }
    }

    fun buildAddress(messageResource: Resource, predicate: IRI, mailbox: Mailbox): List<Statement> {
        val from = VF.createBNode()
        return arrayListOf(VF.createStatement(messageResource, predicate, from),
                mailbox.name?.let { VF.createStatement(from, FOAF.NAME, VF.createLiteral(it)) },
                VF.createStatement(from, FOAF.MBOX, VF.createIRI("mailto:" + mailbox.address)))
                .filterNotNull()
    }

    /**
     * Convert a message to an RDF model
     */
    fun messageToModel(message: Message): List<Statement> {
        val statements = ArrayList<Statement>()
        val messageResource = VF.createIRI(IRI_PREFIX, strip(message.messageId))
        val msgStmt = { p: IRI, o: Value -> statements.add(VF.createStatement(messageResource, p, o)) }
        msgStmt(RDF.TYPE, MESSAGE)
        extractBody(message.body)?.let { msgStmt(BODY, VF.createLiteral(it)) }
        msgStmt(SUBJECT, VF.createLiteral(message.subject))
        msgStmt(DATE, VF.createLiteral(message.date))
        message.from.forEach { statements.addAll(buildAddress(messageResource, FROM, it)) }
        // supposed to work in Kotlin-1.1: //message.from.map { buildAddress(messageResource, it) }.forEach(statements::addAll)
        message.to.flatten().forEach { statements.addAll(buildAddress(messageResource, TO, it)) }

        message.header.let {
            it.getField("in-reply-to")?.let {
                msgStmt(IN_REPLY_TO, VF.createIRI(IRI_PREFIX, strip((it as UnstructuredField).value)))
            }
            it.getField("references")?.let {
                val refsStmt = { r: String -> msgStmt(REFERENCES, VF.createIRI(IRI_PREFIX, r)) }
                (it as UnstructuredField).value.split(Regex("\\s+")).map(strip).map(refsStmt)
            }
        }
        return statements
    }

    /**
     * Implement the extractor interface by creating a statement iterator
     */
    override fun extract(databaseConnection: DatabaseConnection?, docIri: IRI, path: Path): StatementSource {
        // check for qmail style message delimiters
        val isCollection = Files.newInputStream(path).use {
            val bytes = ByteArray(5)
            it.read(bytes)
            String(bytes) == "From "
        }

        // use the Apache James `MboxIterator' if we have a qmail style message collection
        val messageIterator = if (isCollection)
            MboxIterator.fromFile(path.toFile()).build().map { it.asInputStream(Charsets.UTF_8) }
        else
            arrayListOf(Files.newInputStream(path))

        // iterator over all statements generated from the message(s)
        val stmtIterator = messageIterator
                .map { messageBuilder.parseMessage(it) }
                .flatMap { messageToModel(it) }
                .iterator()

        // build a `StatementSource' to implement the `RDFExtractor' interface
        return object : AbstractStatementSource() {
            override fun statements() = DefaultStatementIterator(stmtIterator)
        }
    }
}
