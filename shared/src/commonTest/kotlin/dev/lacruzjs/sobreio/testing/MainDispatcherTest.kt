package dev.lacruzjs.sobreio.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/** Replaces Dispatchers.Main in ViewModel tests: `class XViewModelTest : MainDispatcherTest()`. */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class MainDispatcherTest(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) {
    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(testDispatcher)

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()
}
