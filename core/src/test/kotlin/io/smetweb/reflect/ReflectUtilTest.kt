package io.smetweb.reflect

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReflectUtilTest {

    open class MyClass

    class SubClass: MyClass()

    interface Parameter

    interface ParameterizedInterface<X: Any>

    enum class MyEnum: ParameterizedInterface<MyClass> { CONSTANT }

    open class ParameterizedSubclass<Y: Parameter>: MyClass(), ParameterizedInterface<Array<MyClass>>

    class FinalSubclass: ParameterizedSubclass<Parameter>()

    @Test
    fun `test Type toClass`() {
        assertEquals(MyClass::class.java, MyClass::class.java.toClass())

        val testParameterized = ParameterizedSubclass<Parameter>()
        assertEquals(ParameterizedSubclass::class.java, testParameterized::class.java.toClass())

        val testArray = java.lang.reflect.Array.newInstance(ParameterizedInterface::class.java, 0)
        assertEquals(arrayOf<ParameterizedInterface<Nothing>>()::class.java, testArray::class.java.toClass())
    }

    @Test
    fun `test Type resolveParameterizedType`() {
        val testEnum = MyEnum.CONSTANT

        val testEnumType = testEnum::class.java.resolveParameterizedType(ParameterizedInterface::class.java)
        assertEquals(ParameterizedInterface::class.java, testEnumType.rawType)
        assertEquals(listOf(MyClass::class.java), testEnumType.actualTypeArguments.toList())

        val testFinal = arrayOf(FinalSubclass())

        val testFinalSubType = testFinal::class.java.resolveParameterizedType(ParameterizedSubclass::class.java)
        assertEquals(ParameterizedSubclass::class.java, testFinalSubType.rawType)
        assertEquals(Parameter::class.java, testFinalSubType.actualTypeArguments[0])

        val testFinalInterface = testFinal::class.java.resolveParameterizedType(ParameterizedInterface::class.java)
        assertEquals(ParameterizedInterface::class.java, testFinalInterface.rawType)
        assertEquals(Array<MyClass>::class.java, testFinalInterface.actualTypeArguments[0])

        assertEquals(MyClass::class.java, testFinalInterface.resolveTypeArguments()[0].second.componentType)
    }
}