package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import org.junit.jupiter.api.Test
import kotlin.reflect.KProperty1

class ShortDslTest {

    data class Domain(
        val aString: String,
        val anInt: Int
    )

    object Converter : JAny<Domain>() {
        val aString by str(Domain::aString)
        val anInt by num(Domain::anInt)

        override fun JsonNodeObject.deserializeOrThrow() =
            Domain(+aString, +anInt)
    }

    @Test fun test() {
        val d = Domain("banana", 42)
        val t = Converter.toPrettyJson(d)
        println(t)
    }

    @Test fun short() {
        val d = Domain("banana", 42)
        val c: JsonConverter<Domain, JsonNodeObject> =
            converter(
                ::Domain,
                Domain::aString,
                Domain::anInt,
            )
        val t = Converter.toJsonNode(d, NodePathRoot)
        println(t)
    }
}

inline fun <D: Any, reified P1, reified P2> converter(
    ctor: (P1, P2) -> D,
    p1: KProperty1<D, P1>,
    p2: KProperty1<D, P2>,
): JsonConverter<D, JsonNodeObject> = object : JAny<D>() {

    init {
        registerProperty(property)
    }

    override fun JsonNodeObject.deserializeOrThrow() = ctor(
        ShortDslTest.Domain(+aString, +anInt)

}