package com.ubertob.kondor.loadTests

import com.ubertob.kondor.chronoAndLog
import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.jsonnode.ArrayNode
import com.ubertob.kondor.json.jsonnode.onRoot
import com.ubertob.kondor.json.parser.KondorTokenizer
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

/*
On my laptop: 4/5/2023

JInvoices 50k Invoices, 63MB
serialization 1300 ms
serialization compact 1157 ms
total parsing 1339 ms
tokenizing 672 ms
toJsonNode 524 ms
marshalling 224 ms


JFileInfo 100k 15MB
serialization 282 ms
serialization compact 220 ms
total parsing 470 ms
tokenizing 163 ms
toJsonNode 303 ms
marshalling 70 ms

JStrings 100k 1.6Mb
serialization 11 ms
serialization compact 10 ms
total parsing 20 ms
tokenizing 31 ms
toJsonNode 22 ms
marshalling 3 ms

 */

@Disabled
class PerformanceTest {

    @Test
    fun `serialize and parse invoices`() {

        val JInvoices = JList(JInvoice)

        val invoices = generateSequence(0) { it + 1 }.take(50_000).map {
            randomInvoice().copy(id = InvoiceId(it.toString()))
        }.toList()

        println("Json String length ${JInvoices.toJson(invoices).length}")
        repeat(100) {

            val jsonString = chronoAndLog("serialization") { JInvoices.toJson(invoices) }

            chronoAndLog("serialization compact") { JInvoices.toJson(invoices, JsonStyle.compact) }

            chronoAndLog("total parsing") { JInvoices.fromJson(jsonString) }

            val tokens = chronoAndLog("tokenizing") { KondorTokenizer.tokenize(jsonString) } //add for each for lazy

            val nodes = chronoAndLog("toJsonNode") { ArrayNode.parse(tokens.onRoot()) }.expectSuccess()

            chronoAndLog("marshalling") { JInvoices.fromJsonNode(nodes) }

        }

    }


    @Test
    fun `serialize and parse FileInfo`() {

        val jFileInfos = JList(JFileInfo)

        val fileInfos = generateSequence(0) { it + 1 }.take(100_000).map {
            randomFileInfo().copy(name = it.toString())
        }.toList()

        println("Json String length ${jFileInfos.toJson(fileInfos).length}")
        repeat(100) {

            val jsonString = chronoAndLog("serialization") { jFileInfos.toJson(fileInfos) }

            chronoAndLog("serialization compact") { jFileInfos.toJson(fileInfos, JsonStyle.compact) }

            chronoAndLog("total parsing") { jFileInfos.fromJson(jsonString) }

            val tokens =
                chronoAndLog("tokenizing") { KondorTokenizer.tokenize(jsonString) } //add for eaJFileInfosch for lazy

            val nodes = chronoAndLog("toJsonNode") { ArrayNode.parse(tokens.onRoot()) }.expectSuccess()

            chronoAndLog("marshalling") { jFileInfos.fromJsonNode(nodes) }

        }

    }

    @Test
    fun `serialize and parse array of strings`() {

        val jStrings = JList(JString)

        val strings = generateSequence(0) { it + 1 }.take(100_000).map {
            "string $it"
        }.toList()

        println("Json String length ${jStrings.toJson(strings).length}")
        repeat(100) {

            val jsonString = chronoAndLog("serialization") { jStrings.toJson(strings) }

            chronoAndLog("serialization compact") { jStrings.toJson(strings, JsonStyle.compact) }


            chronoAndLog("total parsing") { jStrings.fromJson(jsonString) }

            val tokens =
                chronoAndLog("tokenizing") { KondorTokenizer.tokenize(jsonString) } //add for eaJFileInfosch for lazy

            val nodes = chronoAndLog("toJsonNode") { ArrayNode.parse(tokens.onRoot()) }.expectSuccess()

            chronoAndLog("marshalling") { jStrings.fromJsonNode(nodes) }

        }

    }


    @Test
    fun `using inputstream to parse invoices`() {

        val JInvoices = JList(JInvoice)

        val fixtureName = "/fixtures/invoices.json.ignoreme"

//        val invoices = generateSequence(0) { it + 1 }.take(500_000).map {
//            randomInvoice().copy(id = InvoiceId(it.toString()))
//        }.toList()
//
//        File("./src/test/resources/$fixtureName").writeText(JInvoices.toJson(invoices))


        val inputStream = javaClass.getResourceAsStream(fixtureName) ?: error("resource $fixtureName not found!")

        chronoAndLog("parsing from stream") {
            val invoices = JInvoices.fromJson(inputStream).expectSuccess()
            expectThat(invoices.size).isEqualTo(500_000)
        }

    }
}



