package com.ubertob.kondor.json

import com.ubertob.kondor.json.datetime.JLocalDate
import com.ubertob.kondor.json.jsonnode.JsonNode
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1


data class Address(
    val line1: String,
    val line2: String
)

data class Order(
    val date: LocalDate,
    val address: Address
)

object JAddress : JAny<Address>() {
    val line1 by str(Address::line1)
    val line2 by str(Address::line2)

    override fun JsonNodeObject.deserializeOrThrow() =
        Address(+line1, +line2)
}

class JOrder(context: Context) : JAny<Order>() {
    val address by context(Order::address)
    val placed by context(Order::date)

    override fun JsonNodeObject.deserializeOrThrow() =
        Order(+placed, +address, )
}


class ContextTests {

    @Test
    fun `round trip`() {
        val order = Order(LocalDate.now(), Address("line1", "line2"))

        with(Context) {
            val json: String = order.toJson()
            val back: Order = json.to<Order>().orThrow()
            assertEquals(order, back)
        }
    }
}


object Context : AbstractConversionContext() {

    private val localDate = JLocalDate.withPattern("dd/MM/yyyy")
    private val order = JOrder(this)

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> converterFor(c: KClass<T>) = when (c) {
        LocalDate::class -> localDate
        Order::class -> order
        Address::class -> JAddress
        else -> error("Unexpected class ${c.qualifiedName}")
    } as JsonConverter<T, JsonNode>
}

abstract class AbstractConversionContext {
    inline operator fun <K: Any, reified T: Any> invoke(prop: KProperty1<K, T>) =
        JField(prop, Context.converterFor(T::class))

    inline fun <reified T: Any> T.toJson() = converterFor(T::class).toJson(this)
    inline fun <reified T: Any> String.to() = converterFor(T::class).fromJson(this)

    abstract fun <T: Any> converterFor(c: KClass<T>): JsonConverter<T, JsonNode>
}

