package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.JsonNodeString
import com.ubertob.kondor.json.jsonnode.NodePathSegment
import com.ubertob.kondor.json.schema.enumSchema
import com.ubertob.kondor.json.schema.sealedSchema
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import kotlin.reflect.KClass


interface StringWrapper {
    val raw: String
}

data class JStringWrapper<T : StringWrapper>(override val cons: (String) -> T) : JStringRepresentable<T>() {
    override val render: (T) -> String = { it.raw }
}

object JBigDecimal : JNumRepresentable<BigDecimal>() {
    override val cons: (BigDecimal) -> BigDecimal = { it }
    override val render: (BigDecimal) -> BigDecimal = { it }
}

object JBigInteger : JNumRepresentable<BigInteger>() {
    override val cons: (BigDecimal) -> BigInteger = BigDecimal::toBigInteger
    override val render: (BigInteger) -> BigDecimal = BigInteger::toBigDecimal
}


object JCurrency : JStringRepresentable<Currency>() {
    override val cons: (String) -> Currency = Currency::getInstance
    override val render: (Currency) -> String = Currency::getCurrencyCode
}


data class JEnum<E : Enum<E>>(override val cons: (String) -> E, val values: List<E> = emptyList()) :
    JStringRepresentable<E>() {
    override val render: (E) -> String = { it.name } //see enumValueOf() and enumValues()
    override fun schema(): JsonNodeObject = enumSchema(values)
}

data class JEnumClass<E : Enum<E>>(val clazz: KClass<E>) :
    JStringRepresentable<E>() {
    private val valuesMap: Map<String, E> by lazy { clazz.java.enumConstants.associateBy { it.name } }
    override val cons: (String) -> E = { name -> valuesMap[name] ?: error("not found $name among ${valuesMap.keys}") }
    override val render: (E) -> String = { it.name }
    override fun schema(): JsonNodeObject = enumSchema(valuesMap.values.toList())
}

//for serializing Kotlin object and other single instance types
data class JInstance<T : Any>(val singleton: T) : JAny<T>() {
    override fun JsonNodeObject.deserializeOrThrow() = singleton
}

abstract class JSealed<T : Any> : PolymorphicConverter<T>() {

    open val discriminatorFieldName: String = "_type"

    open val defaultConverter: ObjectNodeConverterWriters<out T>? = null

    fun typeWriter(jno: JsonNodeObject, obj: T): JsonNodeObject =
        jno.copy(
            _fieldMap = mutableMapOf(
                discriminatorFieldNode(obj, jno)
            ).apply { putAll(jno._fieldMap) }
        )

    private fun discriminatorFieldNode(
        obj: T,
        jno: JsonNodeObject
    ): NamedNode = discriminatorFieldName to
            JsonNodeString(extractTypeName(obj), NodePathSegment(discriminatorFieldName, jno._path))

    override fun JsonNodeObject.deserializeOrThrow(): T? {


        val discriminatorNode = _fieldMap[discriminatorFieldName]
            ?: defaultConverter?.let { return it.fromJsonNode(this).orThrow() }
            ?: error("expected discriminator field \"$discriminatorFieldName\" not found")

        val typeName = JString.fromJsonNodeBase(discriminatorNode).orThrow()
        val converter = subConverters[typeName] ?: error("subtype not known $typeName")
        return converter.fromJsonNode(this).orThrow()
    }

    override fun getWriters(value: T): List<NodeWriter<T>> =
        extractTypeName(value).let { typeName ->
            findSubTypeConverter(typeName)
                ?.getWriters(value)
                ?.plus(::typeWriter)
                ?: error("subtype not known $typeName")
        }

    override fun schema() = sealedSchema(discriminatorFieldName, subConverters)

}

//to map polimorphic object with xml->json standard convention
abstract class NestedPolyConverter<T : Any> : PolymorphicConverter<T>() {

    override fun JsonNodeObject.deserializeOrThrow(): T {
        val typeName = _fieldMap.keys.first()
        val converter = subConverters[typeName] ?: error("subtype not known $typeName")
        return converter.fromJsonNode(this).orThrow()
    }

    override fun getWriters(value: T): List<NodeWriter<T>> =
        extractTypeName(value).let { typeName ->
            findSubTypeConverter(typeName)
                ?.getWriters(value)
                ?: error("subtype not known $typeName")
        }
}

