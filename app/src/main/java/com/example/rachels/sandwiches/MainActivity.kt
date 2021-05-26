package com.example.rachels.sandwiches

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

class MainActivity : AppCompatActivity() {
    private val KEY_BUNDLE_ORDER_ITEM = "com.example.rachels.sandwiches.KEY_BUNDLE_ORDER_ITEM"
    private val PREFERENCE_KEY = "com.example.rachels.sandwiches.PREFERENCE_FILE_KEY"
    private val KEY_SP_ORDER_ID = "com.example.rachles.sandwiches.SP_ORDER_ITEM_ID_KEY"
    private val KEY_SP_PREVIUOS_USER_NAME = "com.example.rachles.sandwiches.KEY_SP_PREVIUOS_USER_NAME"

    private lateinit var sharedPref: SharedPreferences

    private lateinit var currentDisplayingScreen: Screen
    private lateinit var sandwichInfoLayout: SandwichInfoLayout
    private lateinit var newOrderScreen: NewOrderScreen
    private lateinit var editOrderScreen: EditOrderScreen
    private lateinit var inProgressScreen: InProgressScreen
    private lateinit var readyScreen: ReadyScreen

    private lateinit var loadingBar: ProgressBar

    var currentOrderManager : OrderManager? = null  // Exposed for tests purposes...

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPref = getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE)

        sandwichInfoLayout = SandwichInfoLayout(this).apply { dismiss() }
        newOrderScreen = NewOrderScreen(this, ::savePreferredName).apply { dismiss() }
        currentDisplayingScreen = newOrderScreen
        editOrderScreen = EditOrderScreen(this).apply { dismiss() }
        inProgressScreen = InProgressScreen(this).apply { dismiss() }
        readyScreen = ReadyScreen(this).apply { dismiss() }
        loadingBar = findViewById<ProgressBar>(R.id.progressBar).apply { visibility = View.GONE}

        newOrderScreen.setOnSubmitListener { createNewOrder() }
        editOrderScreen.also {
            it.setOnSubmit{ updateOrder() }
            it.setOnDelete{ deleteOrder() }
        }

        readyScreen.setOnClick {
            loadingBar.visibility = View.VISIBLE
            currentOrderManager?.setDone()
                ?.addOnSuccessListener {
                    loadingBar.visibility = View.GONE
                    showNewOrderScreen()
                    sandwichInfoLayout.reset()
                }
        }

        loadOrCreateOrderManager(savedInstanceState)
    }

    private fun loadOrCreateOrderManager(savedInstanceState: Bundle?) {
        loadingBar.visibility = View.VISIBLE
        if (currentOrderManager != null) {
            loadingBar.visibility = View.GONE
        }

        if (currentOrderManager == null && savedInstanceState?.containsKey(KEY_BUNDLE_ORDER_ITEM) == true) {
            // Try to get manager from instance bundle
            loadingBar.visibility = View.GONE
            val orderItem = savedInstanceState.getSerializable(KEY_BUNDLE_ORDER_ITEM) as? OrderItem
            if (orderItem != null) {
                currentOrderManager = collectFromExistingOrder(orderItem)
                    .apply { registerToStateChange(::onOrderStateChange) }
                onOrderStateChange(currentOrderManager!!.status)
            } else {
                showNewOrderScreen()
            }
            return
        }

        if (currentOrderManager == null) {
            // Try to get manager from the hard drive
            GlobalScope.launch {
                val id = sharedPref.getString(KEY_SP_ORDER_ID, null)
                currentOrderManager = id?.let{ collectFromExistingOrderID(id) }
                    ?.apply { registerToStateChange(::onOrderStateChange) }
                runOnUiThread {
                    loadingBar.visibility = View.GONE
                    if (currentOrderManager == null) {
                        showNewOrderScreen()
                        sandwichInfoLayout.reset()
                    } else {
                        onOrderStateChange(currentOrderManager!!.status)
                    }
                }
            }
        }
    }

    private fun createNewOrder(){
        try {
            loadingBar.visibility = View.VISIBLE
            newOrderScreen.isSubmitEnabled = false
            currentOrderManager = with(sandwichInfoLayout) {
                createNewOrder(newOrderScreen.nameInput, picklesNumber, hummusFlag, tahiniFlag, comment)
            }.also {
                it.registerToStateChange { state -> onOrderStateChange(state) }
                it.uploadToDB()
                    .addOnSuccessListener {
                        showEditOrderScreen()
                        loadingBar.visibility = View.GONE
                        newOrderScreen.isSubmitEnabled = true
                    }
                    .addOnFailureListener {
                        loadingBar.visibility = View.GONE
                        newOrderScreen.isSubmitEnabled = true
                        currentOrderManager = null
                        makeToast("Couldn't create new order. Try again later")
                    }
            }
        }catch (e: IllegalArgumentException){
            makeToast(e.message ?: "Couldn't create new order. Check your input!")
            loadingBar.visibility = View.GONE
            newOrderScreen.isSubmitEnabled = true
        }
    }

    private fun updateOrder() {
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

    private fun deleteOrder() {
        currentOrderManager?.unregisterAll()
        currentOrderManager?.deleteOrder()
        currentOrderManager = null
        showNewOrderScreen()
        sandwichInfoLayout.reset()
    }

    private fun onOrderStateChange(state: String){
        when (state){
            WAITING -> showEditOrderScreen()
            IN_PROGRESS -> showInProgressScreen()
            READY -> showReadyScreen()
            DONE -> {
                showNewOrderScreen()
                sandwichInfoLayout.reset()
            }
        }
    }

    private fun showNewOrderScreen() {
        currentDisplayingScreen.dismiss()

        newOrderScreen.show()
        newOrderScreen.nameInput = sharedPref.getString(KEY_SP_PREVIUOS_USER_NAME, "") ?: ""

        sandwichInfoLayout.show()

        currentDisplayingScreen = newOrderScreen
    }

    private fun showEditOrderScreen() {
        currentDisplayingScreen.dismiss()

        editOrderScreen.show()
        sandwichInfoLayout.show()
        with(sandwichInfoLayout) {
            comment = currentOrderManager!!.comment
            picklesNumber = currentOrderManager!!.picklesNum
            hummusFlag = currentOrderManager!!.hummusFlag
            tahiniFlag = currentOrderManager!!.tahiniFlag
        }

        currentDisplayingScreen = editOrderScreen
    }

    private fun showInProgressScreen() {
        inProgressScreen.apply {
            setThankUName(currentOrderManager?.name ?: "Stranger")
            picklesNum = currentOrderManager?.picklesNum ?: 0
            hummusFlag = currentOrderManager?.hummusFlag ?: false
            tahiniFlag = currentOrderManager?.tahiniFlag ?: false
        }

        currentDisplayingScreen.dismiss()
        sandwichInfoLayout.dismiss()

        inProgressScreen.show()
        currentDisplayingScreen = inProgressScreen

    }

    private fun showReadyScreen() {
        currentDisplayingScreen.dismiss()
        sandwichInfoLayout.dismiss()

        readyScreen.show()
        currentDisplayingScreen = readyScreen
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

    override fun onPause() {
        super.onPause()
        with(sharedPref.edit()) {
            putString(KEY_SP_ORDER_ID, currentOrderManager?.id)
            apply()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(KEY_BUNDLE_ORDER_ITEM, currentOrderManager?.orderItem?:" ")
    }

    private fun savePreferredName(name: String){
        Log.d(TAG,name)
        sharedPref.edit().putString(KEY_SP_PREVIUOS_USER_NAME, name).apply()
        Log.d(TAG, sharedPref.getString(KEY_SP_PREVIUOS_USER_NAME, null)?:"NULL")
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

    var comment: String
        get() = commentInput.text.toString()
        set(value) = commentInput.setText(value)

    fun clearComment() =  commentInput.text?.clear()

    override fun show() {
        layout.visibility = View.VISIBLE
    }

    override fun dismiss() {
        layout.visibility = View.GONE
    }

    fun reset() {
        picklesNumber = 3
        hummusFlag = false
        tahiniFlag = false
        clearComment()
    }
}

class NewOrderScreen (activity: MainActivity, val onNameChange: (String)->Unit) : Screen {
    private val topLayout: LinearLayout = activity.findViewById(R.id.add_new_order_screen_top)
    private val nameTextInput : TextInputEditText = activity.findViewById(R.id.newOrder__customer_name_edit_text)
    private val submitButton : View = activity.findViewById(R.id.newOrder__submit_button)

    init {
        nameTextInput.addTextChangedListener { watcher -> onNameChange(nameInput) }
    }

    var isSubmitEnabled: Boolean
        get() = submitButton.isEnabled
        set(value) { submitButton.isEnabled = value }

    fun setOnSubmitListener(onClick: View.OnClickListener) {
        submitButton.setOnClickListener(onClick)
    }

    var nameInput: String
        get() = nameTextInput.text.toString()
        set(value) {
            onNameChange(value)
            nameTextInput.setText(value)
        }


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

class InProgressScreen (private val activity: MainActivity) : Screen {
    private val layout: LinearLayout = activity.findViewById(R.id.inprogress_layout)
    private val thankuTextView: TextView = activity.findViewById(R.id.inProgress__thanku_message)
    private val picklesTextView: TextView = activity.findViewById(R.id.inProgress__pickles_li)
    private val hummusTextView: TextView = activity.findViewById(R.id.inProgress__hummus_li)
    private val tahiniTextView: TextView = activity.findViewById(R.id.inProgress__tahini_li)

    var picklesNum: Int = 0
        set(value) { picklesTextView.text = activity.resources.getQuantityString(R.plurals.pickles_num_plural, value, value) }

    var hummusFlag: Boolean = false
        set(value) { hummusTextView.text = activity.getString(if (value) R.string.with_hummus else R.string.no_hummus) }

    var tahiniFlag: Boolean = false
        set(value) { tahiniTextView.text = activity.getString(if (value) R.string.with_tahini else R.string.no_tahini) }


    fun setThankUName(name: String){
        thankuTextView.text = activity.resources.getString(R.string.thanku_message, name)
    }

    override fun show() {
        layout.visibility = View.VISIBLE
    }

    override fun dismiss() {
        layout.visibility = View.GONE
    }

}

class ReadyScreen (activity: MainActivity) : Screen {
    private val layout: LinearLayout = activity.findViewById(R.id.order_ready_layout)
    private val button: Button = activity.findViewById(R.id.orderReady__button)

    fun setOnClick(l: View.OnClickListener) {
        button.setOnClickListener(l)
    }

    override fun show() {
        layout.visibility = View.VISIBLE
    }

    override fun dismiss() {
        layout.visibility = View.GONE
    }
}

const val TAG = "DEBUG_TAG"