package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import kotlin.reflect.KProperty1

object A {
    private data class Inner(
        val name: String
    )

    private data class Outer(
        val inner: Inner
    )

    private object JInner : JAny<Inner>() {
        val name by str(Inner::name)
        override fun JsonNodeObject.deserializeOrThrow() =
            Inner(+name)
    }

    private class JOuter(context: Context) : JAny<Outer>() {
        val inner by obj(context.inner, Outer::inner)
        override fun JsonNodeObject.deserializeOrThrow() =
            Outer(+inner)
    }

    private object Context {
        val inner = JInner
        val outer = JOuter(this)

        fun Inner.toJson() = JInner.toJson(this)

        @JvmName("isdif")
        operator fun invoke(ignore: KProperty1<*, Outer>): ObjectNodeConverter<Outer> = JOuter(this)
    }

}