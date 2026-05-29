package com.example.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.viewmodel.FinanceViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput

@RunWith(RobolectricTestRunner::class)
class DashboardScreenCrashTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDashboardScreen() {
        var exceptionMsg: Throwable? = null
        try {
            composeTestRule.setContent {
                val db = com.example.data.AppDatabase.getDatabase(androidx.test.core.app.ApplicationProvider.getApplicationContext())
                val repo = com.example.data.AppRepository(db.transactionDao(), db.securityDao())
                val viewModel = FinanceViewModel(repo, androidx.test.core.app.ApplicationProvider.getApplicationContext())
                DashboardScreen(viewModel)
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("nav_add").performClick()
            composeTestRule.waitForIdle()
            
            // Try touching the input
            composeTestRule.onNodeWithTag("amount_input").performTextInput("10000")
            composeTestRule.waitForIdle()
            
            // Try touching the title input
            composeTestRule.onNodeWithTag("title_input").performTextInput("Test Bensin")
            composeTestRule.waitForIdle()
            
            // Click save button
            composeTestRule.onNodeWithTag("save_transaction_button").performClick()
            composeTestRule.waitForIdle()

        } catch(e: Throwable) {
            e.printStackTrace()
            exceptionMsg = e
        }
        
        if (exceptionMsg != null) {
            throw java.lang.RuntimeException("CRASH REASON OLA: ", exceptionMsg)
        }
    }
}

