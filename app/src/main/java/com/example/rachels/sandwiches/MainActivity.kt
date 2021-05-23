package com.example.rachels.sandwiches

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.textfield.TextInputEditText
import java.lang.IllegalArgumentException

class MainActivity : AppCompatActivity() {

    private lateinit var newOrderScreen: NewOrderScreen
    var currentOrderManager : OrderManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        newOrderScreen = NewOrderScreen(this)

        if (currentOrderManager == null){
            currentOrderManager = collectExistingOrder("0")
        }
        if (currentOrderManager == null){
            newOrderScreen.show()
        }


        newOrderScreen.setOnSubmitListener { createNewOrder() }

    }

    fun createNewOrder(){
        try {
            currentOrderManager = with(newOrderScreen) {
                createNewOrder(nameInput, picklesNumber, hummusFlag, tahiniFlag, comment)
            }.also {
                it.uploadToDB()
                it.registerToStateChange { state -> onOrderStateChange(state) }
            }
        }catch (e: IllegalArgumentException){
            makeToast(e.message ?: "Couldn't create new order. Check your input!")
        }
    }

    private fun onOrderStateChange(state: String){
        when (state){

            IN_PROGRESS -> {
                newOrderScreen.dismiss()
            }
            READY -> {

            }
            DONE -> {

            }

        }
    }

    private fun makeToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentOrderManager?.unregisterAll()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm: InputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }
}

class NewOrderScreen (activity: MainActivity) {
    private val layout: ConstraintLayout = activity.findViewById(R.id.add_new_order_screen)
    private val nameTextInput : TextInputEditText = activity.findViewById(R.id.newOrder__customer_name_edit_text)
    private val picklesSeekbar : SeekBar = activity.findViewById(R.id.newOrder__pickles_seekbar)
    private val hummusSwitch : SwitchCompat = activity.findViewById(R.id.newOrder__hummus_switch)
    private val tahiniSwitch : SwitchCompat = activity.findViewById(R.id.newOrder__tahini_switch)
    private val commentInput : TextInputEditText = activity.findViewById(R.id.newOrder__comment_text_input)
    private val submitButton : Button = activity.findViewById(R.id.newOrder__submit_button)

    fun setOnSubmitListener(onClick: View.OnClickListener) {
        submitButton.setOnClickListener(onClick)
    }

    var nameInput: String
        get() = nameTextInput.text.toString()
        set(value) = nameTextInput.setText(value)

    var picklesNumber: Int
        get() = picklesSeekbar.progress
        set(value) { picklesSeekbar.progress = value }

    var hummusFlag: Boolean
        get() = hummusSwitch.isChecked
        set(value) { hummusSwitch.isChecked = value}

    var tahiniFlag: Boolean
        get() = tahiniSwitch.isChecked
        set(value) { tahiniSwitch.isChecked = value }

    val comment: String
        get() = commentInput.text.toString()

    fun clearComment() =  commentInput.text?.clear()

    fun show() {
        layout.visibility = View.VISIBLE
    }

    fun dismiss() {
        layout.visibility = View.GONE
    }
}
const val TAG = "DEBUG_TAG"