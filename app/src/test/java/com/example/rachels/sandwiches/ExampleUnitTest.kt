package com.example.rachels.sandwiches

import org.junit.Test

import org.junit.Assert.*
import java.lang.IllegalArgumentException

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun testCreateNewOrderFalseValidation(){
        val invalidOrderItems = listOf(
            OrderItem(customerName = "%&%^&^"),
            OrderItem(customerName = ""),
            OrderItem(customerName = " "),
            OrderItem(customerName = "   \t "),
            OrderItem(customerName = "!!!-+"),
            OrderItem(customerName = "TEST1", picklesNum = -1),
            OrderItem(customerName = "TEST1", picklesNum = 11),
            OrderItem(customerName = "TEST1", picklesNum = 110),
        )

        invalidOrderItems.forEach {
            assertThrows(IllegalArgumentException::class.java) {
                    _validateOrder(it.customerName, it.picklesNum, it.hummusFlag, it.tahiniFlag, it.comment)
            }
        }
    }

    @Test
    fun testCreateNewOrderTrueValidation() {
        val validOrderItems = listOf(
            OrderItem(customerName = "TEST2", picklesNum = 0),
            OrderItem(customerName = "TEST2", picklesNum = 5),
            OrderItem(customerName = "TEST2", picklesNum = 10),
            OrderItem(customerName = "TEST2 TEST2", picklesNum = 10),
            OrderItem(customerName = " TEST2 TEST2", picklesNum = 10),
            OrderItem(customerName = " TEST2 TEST2 TEST2 ", picklesNum = 10),
            OrderItem(customerName = "TEST2", picklesNum = 10, hummusFlag = true, tahiniFlag = true),
            OrderItem(customerName = "TEST2", picklesNum = 10, hummusFlag = true),
            OrderItem(customerName = "TEST2", picklesNum = 10, tahiniFlag = true),
            OrderItem(customerName = "TEST2", picklesNum = 10, comment = "TEST1"),
            OrderItem(customerName = "TEST2", picklesNum = 10, comment = "TEST1".padEnd(100,'1')),
            OrderItem(customerName = "TEST2", picklesNum = 10, comment = "TEST1".padEnd(100,' ')),
        )

        validOrderItems.forEach {
            _validateOrder(it.customerName, it.picklesNum, it.hummusFlag, it.tahiniFlag, it.comment)
        }
    }
}