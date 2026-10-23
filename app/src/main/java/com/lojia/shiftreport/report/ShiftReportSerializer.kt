package com.lojia.shiftreport.report

import com.lojia.shiftreport.data.*
import com.lojia.shiftreport.util.AppLogger
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Handles JSON serialization and deserialization of shift report ledger entries.
 */
object ShiftReportSerializer {

    private const val TAG = "ShiftReportSerializer"

    fun serializeDueCredits(list: List<DueCreditItem>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("customerName", item.customerName)
            obj.put("amount", item.amount)
            obj.put("note", item.note)
            obj.put("phone", item.phone)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeDueCredits(json: String): List<DueCreditItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<DueCreditItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DueCreditItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        customerName = obj.optString("customerName", ""),
                        amount = obj.optLong("amount", 0L),
                        note = obj.optString("note", ""),
                        phone = obj.optString("phone", "")
                    )
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error deserializing due credits: ${e.message}", e)
        }
        return list
    }

    fun serializePrevDues(list: List<PreviousDueCollectionItem>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("customerName", item.customerName)
            obj.put("amount", item.amount)
            obj.put("paymentMode", item.paymentMode)
            obj.put("note", item.note)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializePrevDues(json: String): List<PreviousDueCollectionItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<PreviousDueCollectionItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PreviousDueCollectionItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        customerName = obj.optString("customerName", ""),
                        amount = obj.optLong("amount", 0L),
                        paymentMode = obj.optString("paymentMode", "CASH"),
                        note = obj.optString("note", "")
                    )
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error deserializing previous dues: ${e.message}", e)
        }
        return list
    }

    fun serializeAdvances(list: List<StaffAdvanceItem>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("staffName", item.staffName)
            obj.put("amount", item.amount)
            obj.put("reason", item.reason)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeAdvances(json: String): List<StaffAdvanceItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<StaffAdvanceItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    StaffAdvanceItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        staffName = obj.optString("staffName", ""),
                        amount = obj.optLong("amount", 0L),
                        reason = obj.optString("reason", "")
                    )
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error deserializing staff advances: ${e.message}", e)
        }
        return list
    }

    fun serializeUnpaid(list: List<UnpaidBillItem>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("tableOrOrderRef", item.tableOrOrderRef)
            obj.put("amount", item.amount)
            obj.put("reason", item.reason)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeUnpaid(json: String): List<UnpaidBillItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<UnpaidBillItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    UnpaidBillItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        tableOrOrderRef = obj.optString("tableOrOrderRef", ""),
                        amount = obj.optLong("amount", 0L),
                        reason = obj.optString("reason", "")
                    )
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error deserializing unpaid bills: ${e.message}", e)
        }
        return list
    }

    fun serializePurchased(list: List<PurchasedInventoryItem>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("itemName", item.itemName)
            obj.put("quantity", item.quantity)
            obj.put("unitPrice", item.unitPrice)
            obj.put("totalAmount", item.totalAmount)
            obj.put("paidVia", item.paidVia)
            obj.put("supplier", item.supplier)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializePurchased(json: String): List<PurchasedInventoryItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<PurchasedInventoryItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PurchasedInventoryItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        itemName = obj.optString("itemName", obj.optString("name", "")),
                        quantity = obj.optDouble("quantity", obj.optDouble("qty", 0.0)),
                        unitPrice = obj.optLong("unitPrice", 0L),
                        totalAmount = obj.optLong("totalAmount", obj.optLong("total", 0L)),
                        paidVia = obj.optString("paidVia", "CASH"),
                        supplier = obj.optString("supplier", "")
                    )
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error deserializing purchased items: ${e.message}", e)
        }
        return list
    }
}
