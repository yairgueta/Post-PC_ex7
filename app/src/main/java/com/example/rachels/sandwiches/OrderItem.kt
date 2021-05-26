package com.example.rachels.sandwiches

import java.io.Serializable


const val WAITING = "waiting"
const val IN_PROGRESS = "in-progress"
const val READY = "ready"
const val DONE = "done"

const val ORDER_ID = "id"
const val CUSTOMER_NAME = "costumerName"
const val PICKLES_NUM = "picklesNum"
const val HUMMUS_FLAG = "hummusFlag"
const val TAHINI_FLAG = "tahiniFlag"
const val ORDER_COMMENT = "comment"
const val ORDER_STATUS = "status"

data class OrderItem(
    val id: String = "",
    val customerName: String? = null,
    var picklesNum: Int = 0,
    var hummusFlag: Boolean = false,
    var tahiniFlag: Boolean = false,
    var comment: String? = null,
    var status: String = WAITING) : Serializable