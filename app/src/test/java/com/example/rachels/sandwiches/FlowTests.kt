package com.example.rachels.sandwiches



import android.app.Activity
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast
import com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner::class)
class FlowTests {
    @Test
    fun when_userInputsBlankName_then_shouldCreateNewOrder() {
        val scenario = ActivityScenario.launch<MainActivity>(MainActivity::class.java)
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onActivity {
            val submitButton : Button = it.findViewById(R.id.newOrder__submit_button)
            submitButton.callOnClick()

            assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Please fill your name!")
            assertThat(it.currentOrderManager).isNull()
        }
    }
}