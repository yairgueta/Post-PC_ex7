package com.example.rachels.sandwiches

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.lang.IllegalArgumentException
import java.util.*

/** Do not call this class constructor. Instead call one of the global creators functions in the end. */
class OrderManager (orderItem: OrderItem, _orderItemDocument: DocumentReference? = null) {
    private val _orderItem: OrderItem = orderItem
    private val orderItemDocument : DocumentReference = _orderItemDocument?: Firebase.firestore.collection(ORDERS_DB_NAME).document(orderItem.id)
    private val registrationObjects : MutableList<ListenerRegistration> = mutableListOf()

    val orderItem: OrderItem
        get() = _orderItem.copy()

    val id: String
        get() = _orderItem.id

    val status: String
        get() = _orderItem.status

    val name: String
        get() = _orderItem.customerName

    var picklesNum: Int
        get() = _orderItem.picklesNum
        set(value) { _orderItem.picklesNum = value }

    var hummusFlag: Boolean
        get() = _orderItem.hummusFlag
        set(value) { _orderItem.hummusFlag = value }

    var tahiniFlag: Boolean
        get() = _orderItem.tahiniFlag
        set(value) { _orderItem.tahiniFlag = value }

    var comment: String
        get() = _orderItem.comment
        set(value) { _orderItem.comment = value}

    fun uploadToDB() : Task<Void> {
        return orderItemDocument.set(this._orderItem)
    }

    fun registerToStateChange(listener : (String)->Unit ) {
        val regObj= orderItemDocument.addSnapshotListener { value, error ->
            if (error!=null) return@addSnapshotListener
            if (value == null) return@addSnapshotListener
            if (value.getString(ORDER_STATUS) != _orderItem.status) {
                _orderItem.status = value.getString(ORDER_STATUS)!!
                listener.invoke(_orderItem.status)
            }
        }
        registrationObjects += regObj
    }


    fun changePicklesAmountInDB(amount: Int) : Task<Void> {
        if (amount < 0 || amount > 10)
            throw IllegalArgumentException("$amount amount of pickles is invalid, Pickles amount must be in [0,10]")
        return orderItemDocument.update(PICKLES_NUM, amount).addOnSuccessListener { _orderItem.picklesNum = amount }
    }

    fun changeHummusFlagInDB(hummusFlag: Boolean) : Task<Void>? {
        if (_orderItem.hummusFlag == hummusFlag) return null
        return orderItemDocument.update(HUMMUS_FLAG, hummusFlag).addOnSuccessListener { _orderItem.hummusFlag = hummusFlag }
    }

    fun changeTahiniFlagInDB(tahiniFlag: Boolean) : Task<Void>? {
        if (_orderItem.tahiniFlag == tahiniFlag) return null
        return orderItemDocument.update(TAHINI_FLAG, tahiniFlag).addOnSuccessListener { _orderItem.tahiniFlag = tahiniFlag }
    }

    fun changeCommentInDB(comment: String) : Task<Void> {
        return orderItemDocument.update(ORDER_COMMENT, comment).addOnSuccessListener { _orderItem.comment = comment }
    }

    fun unregisterAll() {
        registrationObjects.forEach { reg -> reg.remove()}
        registrationObjects.clear()
    }

    fun deleteOrder() : Task<Void> {
        unregisterAll()
        return orderItemDocument.delete()
    }

    fun setDone(): Task<Void> {
        _orderItem.status = DONE
        return uploadToDB()
    }
}

val ORDERS_DB_NAME = "orders"

/**
 * Exposed for tests purposes.
 */
fun _validateOrder(
    customerName: String,
    picklesNum: Int,
    hummusFlag: Boolean,
    tahiniFlag: Boolean,
    comment: String = "") {
    if (customerName.isBlank()){
        throw IllegalArgumentException("Please fill your name!")
    }
    if (!customerName.matches(Regex("^[\\w ]+$"))){
        throw IllegalArgumentException("Please fill legal name!")
    }
    if (picklesNum < 0 || picklesNum > 10){
        throw IllegalArgumentException("Pickles number must be between 0 and 10")
    }
}

/**
 * Creates new order and generates an ID for it.
 */
fun createNewOrder (
    customerName: String,
    picklesNum: Int,
    hummusFlag: Boolean,
    tahiniFlag: Boolean,
    comment: String = "") : OrderManager{

    _validateOrder(customerName, picklesNum, hummusFlag, tahiniFlag, comment)

    val id = UUID.randomUUID().toString();
    val orderItem = OrderItem(id, customerName, picklesNum, hummusFlag, tahiniFlag, comment, WAITING)

    return OrderManager(orderItem)
}

/**
 * Create manager from an existing order item.
 */
fun collectFromExistingOrder(orderItem: OrderItem) : OrderManager {
    return OrderManager(orderItem)
}

/**
 * Creates manager from existing order when you have only order ID.
 * Searches for the order item in the database and therefore this function marked as suspend
 * (which awaits for the getting method to finish). Id no valid order item found, returns null.
 */
suspend fun collectFromExistingOrderID(id: String) : OrderManager? {
    var orderItem: OrderItem? = null
    orderItem = Firebase.firestore.collection(ORDERS_DB_NAME).document(id).get().await()?.toObject<OrderItem>()
    return orderItem?.let { OrderManager(it) }
}