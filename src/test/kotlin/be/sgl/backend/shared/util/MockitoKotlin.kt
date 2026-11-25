package be.sgl.backend.shared.util

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers

/*
 * Kotlin-safe Mockito matchers.
 * Mockito's matchers return null, which breaks calls to Kotlin functions with non-null parameters.
 * These register the matcher and return a value Kotlin accepts.
 */

@Suppress("UNCHECKED_CAST")
fun <T> anyValue(): T {
    ArgumentMatchers.any<T>()
    return null as T
}

fun <T> eqValue(value: T): T {
    ArgumentMatchers.eq(value)
    return value
}

@Suppress("UNCHECKED_CAST")
fun <T> argThatValue(predicate: (T) -> Boolean): T {
    ArgumentMatchers.argThat<T> { predicate(it) }
    return null as T
}

@Suppress("UNCHECKED_CAST")
fun <T> captureValue(captor: ArgumentCaptor<T>): T {
    captor.capture()
    return null as T
}

inline fun <reified T : Any> argumentCaptor(): ArgumentCaptor<T> = ArgumentCaptor.forClass(T::class.java)
