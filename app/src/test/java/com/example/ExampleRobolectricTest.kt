package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.Transaction
import com.example.ui.viewmodel.FinanceViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Catatan saku", appName)
  }

  @Test
  fun `test database and viewmodel creation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = AppDatabase.getDatabase(context)
    assertNotNull(database)
    
    val repository = AppRepository(database.transactionDao(), database.securityDao())
    val viewModel = FinanceViewModel(repository, context)
    assertNotNull(viewModel)
  }

  @Test
  fun `test main activity creation`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.setup().get()
    assertNotNull(activity)
  }
}
