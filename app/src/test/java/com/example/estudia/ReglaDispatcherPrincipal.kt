package com.example.estudia

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

// viewModelScope usa Dispatchers.Main, que en un test de JVM no existe
// (no hay hilo de UI). Esta regla lo reemplaza por un dispatcher de test
// antes de cada test y lo restaura al terminar.
@OptIn(ExperimentalCoroutinesApi::class)
class ReglaDispatcherPrincipal(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
