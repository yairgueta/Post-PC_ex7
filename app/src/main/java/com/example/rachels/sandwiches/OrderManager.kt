package com.example.rachels.sandwiches

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.lang.IllegalArgumentException
import java.util.*

class OrderManager (orderItem: OrderItem) {

    private val _orderItem: OrderItem = orderItem
    private val orderItemDocument : DocumentReference = ordersDB.document(orderItem.id)
    private val registrationObjects : MutableList<ListenerRegistration> = mutableListOf()


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

    fun changePicklesAmount(amount: Int) : Task<Void> {
        if (amount < 0 || amount > 10)
            throw IllegalArgumentException("$amount amount of pickles is invalid, Pickles amount must be in [0,10]")
        return orderItemDocument.update(PICKLES_NUM, amount).addOnSuccessListener { _orderItem.picklesNum = amount }
    }

    fun changeHummusFlag(hummusFlag: Boolean) : Task<Void>? {
        if (_orderItem.hummusFlag == hummusFlag) return null
        return orderItemDocument.update(HUMMUS_FLAG, hummusFlag).addOnSuccessListener { _orderItem.hummusFlag = hummusFlag }
    }

    fun changeTahiniFlag(tahiniFlag: Boolean) : Task<Void>? {
        if (_orderItem.tahiniFlag == tahiniFlag) return null
        return orderItemDocument.update(TAHINI_FLAG, tahiniFlag).addOnSuccessListener { _orderItem.tahiniFlag = tahiniFlag }
    }

    fun changeComment(comment: String) : Task<Void> {
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
}

val ordersDB = Firebase.firestore.collection("orders")

fun createNewOrder (
    customerName: String,
    picklesNum: Int,
    hummusFlag: Boolean,
    tahiniFlag: Boolean,
    comment: String = "") : OrderManager{

    if (customerName.isBlank()){
        throw IllegalArgumentException("Please fill your name!")
    }
    if (!customerName.matches(Regex("^[\\w ]+$"))){
        throw IllegalArgumentException("Please fill legal name!")
    }
    if (picklesNum < 0 || picklesNum > 10){
        throw IllegalArgumentException("Pickles number must be between 0 and 10")
    }

    val id = UUID.randomUUID().toString();
    val orderItem = OrderItem(id, customerName, picklesNum, hummusFlag, tahiniFlag, comment, WAITING)

    return OrderManager(orderItem)
}

fun collectExistingOrder(id: String) : OrderManager?{
//    val s = OrderManager();
    return null
}