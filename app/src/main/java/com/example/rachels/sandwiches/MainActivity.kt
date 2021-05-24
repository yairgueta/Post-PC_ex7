package com.example.rachels.sandwiches

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputEditText
import java.lang.IllegalArgumentException

class MainActivity : AppCompatActivity() {

    private lateinit var currentDisplayingScreen: Screen
    private lateinit var sandwichInfoLayout: SandwichInfoLayout
    private lateinit var newOrderScreen: NewOrderScreen
    private lateinit var editOrderScreen: EditOrderScreen

    private lateinit var loadingBar: ProgressBar

    var currentOrderManager : OrderManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sandwichInfoLayout = SandwichInfoLayout(this).apply { dismiss() }
        newOrderScreen = NewOrderScreen(this).apply { dismiss() }
        currentDisplayingScreen = newOrderScreen
        editOrderScreen = EditOrderScreen(this).apply { dismiss() }

        loadingBar = findViewById<ProgressBar>(R.id.progressBar).apply { visibility = View.GONE}

        if (currentOrderManager == null){
            currentOrderManager = collectExistingOrder("0")
        }
        if (currentOrderManager == null){
            showNewOrderScreen()
        }


        newOrderScreen.setOnSubmitListener { createNewOrder() }

        editOrderScreen.also {
            it.setOnSubmit{ updateOrder() }
            it.setOnDelete{ deleteOrder() }
        }
    }

    fun createNewOrder(){
        try {
            loadingBar.visibility = View.VISIBLE


            currentOrderManager = with(sandwichInfoLayout) {
                createNewOrder(newOrderScreen.nameInput, picklesNumber, hummusFlag, tahiniFlag, comment)
            }.also {
                it.registerToStateChange { state -> onOrderStateChange(state) }
                it.uploadToDB()
                    .addOnSuccessListener { showEditOrderScreen(); loadingBar.visibility = View.GONE }
                    .addOnFailureListener {
                        loadingBar.visibility = View.GONE
                        currentOrderManager = null
                        makeToast("Couldn't create new order. Try again later")
                    }
            }
        }catch (e: IllegalArgumentException){
            makeToast(e.message ?: "Couldn't create new order. Check your input!")
        }
    }

    fun updateOrder() {
        loadingBar.visibility = View.VISIBLE
        editOrderScreen.areButtonsEnabled = false

        currentOrderManager!!.apply {
            picklesNum = sandwichInfoLayout.picklesNumber
            tahiniFlag = sandwichInfoLayout.tahiniFlag
            hummusFlag = sandwichInfoLayout.hummusFlag
            comment = sandwichInfoLayout.comment
        }.also {
            it.uploadToDB().addOnCompleteListener {
                loadingBar.visibility = View.GONE
                editOrderScreen.areButtonsEnabled = true
                makeToast("Order has updated!")
            }.addOnFailureListener {
                makeToast("There was an error updating the order.\nTry again later...")
            }
        }
    }

    fun deleteOrder() {
        currentOrderManager?.unregisterAll()
        currentOrderManager?.deleteOrder()
        currentOrderManager = null
        showNewOrderScreen()
    }

    private fun onOrderStateChange(state: String){
        when (state){
            WAITING -> {
                showEditOrderScreen()
            }
            IN_PROGRESS -> {
                showInProgressScreen()
            }
            READY -> {
                showReadyScreen()
            }
            DONE -> {
                showNewOrderScreen()
            }

        }
    }

    private fun showNewOrderScreen() {
        currentDisplayingScreen.dismiss()

        newOrderScreen.show()
        sandwichInfoLayout.show()

        currentDisplayingScreen = newOrderScreen
    }

    private fun showEditOrderScreen() {
        currentDisplayingScreen.dismiss()

        editOrderScreen.show()
        sandwichInfoLayout.show()

        currentDisplayingScreen = editOrderScreen
    }

    private fun showInProgressScreen() {
        currentDisplayingScreen.dismiss()
        sandwichInfoLayout.dismiss()



    }

    private fun showReadyScreen() {
        currentDisplayingScreen.dismiss()
        sandwichInfoLayout.dismiss()


    }

    private fun makeToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()


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

interface Screen {
    fun show()
    fun dismiss()
}

class SandwichInfoLayout(activity: MainActivity) : Screen {
    private val layout : ConstraintLayout = activity.findViewById(R.id.order_info_layout)
    private val picklesSeekbar : SeekBar = activity.findViewById(R.id.newOrder__pickles_seekbar)
    private val hummusSwitch : SwitchCompat = activity.findViewById(R.id.newOrder__hummus_switch)
    private val tahiniSwitch : SwitchCompat = activity.findViewById(R.id.newOrder__tahini_switch)
    private val commentInput : TextInputEditText = activity.findViewById(R.id.newOrder__comment_text_input)

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

    override fun show() {
        layout.visibility = View.VISIBLE
    }

    override fun dismiss() {
        layout.visibility = View.GONE
    }
}

class NewOrderScreen (activity: MainActivity) : Screen {
    private val topLayout: LinearLayout = activity.findViewById(R.id.add_new_order_screen_top)
    private val nameTextInput : TextInputEditText = activity.findViewById(R.id.newOrder__customer_name_edit_text)
    private val submitButton : View = activity.findViewById(R.id.newOrder__submit_button)

    var isSubmitEnabled: Boolean
        get() = submitButton.isEnabled
        set(value) { submitButton.isEnabled = value }

    fun setOnSubmitListener(onClick: View.OnClickListener) {
        submitButton.setOnClickListener(onClick)
    }

    var nameInput: String
        get() = nameTextInput.text.toString()
        set(value) = nameTextInput.setText(value)


    override fun show() {
        topLayout.visibility = View.VISIBLE
        submitButton.visibility = View.VISIBLE
    }

    override fun dismiss() {
        topLayout.visibility = View.GONE
        submitButton.visibility = View.GONE
    }
}

class EditOrderScreen (activity: MainActivity) : Screen {
    private val topLayout : LinearLayout = activity.findViewById(R.id.edit_order_screen_top)
    private val bottomLayout : LinearLayout = activity.findViewById(R.id.edit_order_screen_bottom)
    private val submitButton : View = activity.findViewById(R.id.editOrder__submit)
    private val deleteButton : View = activity.findViewById(R.id.editOrder__delete_order)

    fun setOnSubmit(l: View.OnClickListener) {
        submitButton.setOnClickListener(l)
    }

    fun setOnDelete(l: View.OnClickListener) {
        deleteButton.setOnClickListener(l)
    }

    var areButtonsEnabled : Boolean
        get() = submitButton.isEnabled && deleteButton.isEnabled
        set(value) {
            submitButton.isEnabled = value
            deleteButton.isEnabled = value
        }

    override fun show() {
        topLayout.visibility = View.VISIBLE
        bottomLayout.visibility = View.VISIBLE
    }

    override fun dismiss() {
        topLayout.visibility = View.GONE
        bottomLayout.visibility = View.GONE
    }
}
const val TAG = "DEBUG_TAG"