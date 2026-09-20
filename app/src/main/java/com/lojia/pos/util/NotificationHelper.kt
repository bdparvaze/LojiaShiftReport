package com.lojia.pos.util


import com.lojia.pos.R


import android.app.NotificationChannel

import android.app.NotificationManager

import android.app.PendingIntent

import android.content.Context

import android.content.Intent

import android.os.Build


import androidx.core.app.NotificationCompat


import androidx.core.app.NotificationManagerCompat

import com.lojia.pos.MainActivity

object NotificationHelper {
    private const val CHANNEL_SHIFT = "lojia_shift_channel"
    private const val CHANNEL_INVENTORY = "lojia_inventory_channel"
    private const val CHANNEL_SYSTEM = "lojia_system_channel"

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val shiftChannel = NotificationChannel(
                CHANNEL_SHIFT,
                "Shift & Sales Reports",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily shift closing summaries and revenue alerts"
            }

            val inventoryChannel = NotificationChannel(
                CHANNEL_INVENTORY,
                "Inventory & Low Stock Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time alerts when product inventory runs below minimum threshold"
            }

            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM,
                "System & Security",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Backup reminders, security notices, and hardware status"
            }

            notificationManager.createNotificationChannels(listOf(shiftChannel, inventoryChannel, systemChannel))
        }
    }

    fun sendLowStockNotification(
        context: Context,
        productName: String,
        currentStock: Double,
        minStockAlert: Double,
        unit: String = "pcs"
    ) {
        initNotificationChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notif_low_stock_title, productName)
        val descriptionText = context.getString(R.string.notif_low_stock, currentStock.toInt(), unit, minStockAlert.toInt(), unit)

        val notification = NotificationCompat.Builder(context, CHANNEL_INVENTORY)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(descriptionText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$descriptionText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(productName.hashCode(), notification)
        } catch (e: SecurityException) {
            // Notification permission might not be granted yet
        }
    }

    fun sendShiftSummaryNotification(
        context: Context,
        cashierName: String,
        shift: String,
        totalSales: Double,
        netCash: Double,
        currency: String
    ) {
        initNotificationChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notif_shift_summary_title, shift)
        val message = context.getString(R.string.notif_shift_summary_desc, cashierName, "%.2f %s".format(totalSales, currency), "%.2f %s".format(netCash, currency))

        val notification = NotificationCompat.Builder(context, CHANNEL_SHIFT)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$message"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(2001, notification)
        } catch (e: SecurityException) {
            // Notification permission might not be granted
        }
    }

    fun sendShiftOpenedNotification(
        context: Context,
        cashierName: String,
        startingCash: Double,
        currency: String
    ) {
        initNotificationChannels(context)
        val title = context.getString(R.string.notif_shift_opened_title)
        val message = context.getString(R.string.notif_shift_opened_desc, cashierName, "%.2f %s".format(startingCash, currency))

        val notification = NotificationCompat.Builder(context, CHANNEL_SHIFT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(2002, notification)
        } catch (e: SecurityException) {
            // Notification permission
        }
    }

    fun sendShiftClosedNotification(
        context: Context,
        cashierName: String,
        variance: Double,
        currency: String
    ) {
        initNotificationChannels(context)
        val statusText = if (variance >= 0) "+%.2f %s".format(variance, currency) else "%.2f %s".format(variance, currency)
        val title = context.getString(R.string.notif_shift_closed_title)
        val message = context.getString(R.string.notif_drawer_closed, statusText)

        val notification = NotificationCompat.Builder(context, CHANNEL_SHIFT)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(2003, notification)
        } catch (e: SecurityException) {
            // Notification permission
        }
    }

    fun sendTestPushNotification(context: Context, title: String, message: String) {
        initNotificationChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_SYSTEM)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(3001, notification)
        } catch (e: SecurityException) {
            // Notification permission
        }
    }
}
