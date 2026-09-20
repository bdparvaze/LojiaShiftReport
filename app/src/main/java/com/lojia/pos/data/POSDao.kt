package com.lojia.pos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface POSDao {
    @Query("SELECT * FROM pos_categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<POSCategory>>

    @Query("SELECT * FROM pos_categories ORDER BY name ASC")
    suspend fun getAllCategoriesList(): List<POSCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<POSCategory>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: POSCategory): Long

    @Delete
    suspend fun deleteCategory(category: POSCategory)

    @Query("SELECT * FROM pos_products WHERE active = 1 ORDER BY name ASC")
    fun getAllProducts(): Flow<List<POSProduct>>

    @Query("SELECT * FROM pos_products")
    suspend fun getAllProductsList(): List<POSProduct>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<POSProduct>)

    @Query("SELECT * FROM pos_products WHERE active = 1")
    suspend fun getAllProductsOnce(): List<POSProduct>

    @Query("SELECT * FROM pos_products WHERE stockQuantity <= minStockAlert AND active = 1 ORDER BY name ASC")
    suspend fun getLowStockProductsOnce(): List<POSProduct>

    @Query("SELECT * FROM pos_products WHERE stockQuantity <= minStockAlert AND active = 1 ORDER BY name ASC")
    fun getLowStockProducts(): Flow<List<POSProduct>>

    @Query("SELECT * FROM pos_products WHERE categoryId = :catId AND active = 1 ORDER BY name ASC")
    fun getProductsByCategory(catId: Int): Flow<List<POSProduct>>

    @Query("SELECT * FROM pos_products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Int): POSProduct?

    @Query("SELECT * FROM pos_products WHERE (barcode = :barcode OR CAST(id AS TEXT) = :barcode) AND active = 1 LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): POSProduct?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: POSProduct): Long

    @Update
    suspend fun updateProduct(product: POSProduct)

    @Query("UPDATE pos_products SET stockQuantity = stockQuantity + :quantityChange WHERE id = :productId")
    suspend fun updateStock(productId: Int, quantityChange: Double)

    @Delete
    suspend fun deleteProduct(product: POSProduct)

    // Modifiers & Discounts
    @Query("SELECT * FROM pos_modifiers WHERE active = 1 ORDER BY name ASC")
    fun getAllModifiers(): Flow<List<POSModifier>>

    @Query("SELECT * FROM pos_modifiers")
    suspend fun getAllModifiersList(): List<POSModifier>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModifiers(modifiers: List<POSModifier>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModifier(modifier: POSModifier): Long

    @Delete
    suspend fun deleteModifier(modifier: POSModifier)

    @Query("SELECT * FROM pos_discounts WHERE active = 1 ORDER BY name ASC")
    fun getAllDiscounts(): Flow<List<POSDiscount>>

    @Query("SELECT * FROM pos_discounts")
    suspend fun getAllDiscountsList(): List<POSDiscount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscounts(discounts: List<POSDiscount>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscount(discount: POSDiscount): Long

    @Delete
    suspend fun deleteDiscount(discount: POSDiscount)

    // Sales
    @Query("SELECT * FROM pos_sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<POSSale>>

    @Query("SELECT * FROM pos_sales ORDER BY timestamp DESC")
    suspend fun getAllSalesList(): List<POSSale>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<POSSale>)

    @Query("SELECT * FROM pos_sales WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getSalesInRange(startTime: Long, endTime: Long): Flow<List<POSSale>>

    @Query("SELECT * FROM pos_sales WHERE id = :saleId LIMIT 1")
    suspend fun getSaleById(saleId: Int): POSSale?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: POSSale): Long

    @Update
    suspend fun updateSale(sale: POSSale)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<POSSaleItem>)

    @Query("SELECT * FROM pos_sale_items")
    suspend fun getAllSaleItemsList(): List<POSSaleItem>

    @Transaction
    suspend fun processCheckoutTransaction(
        sale: POSSale,
        items: List<POSSaleItem>
    ): Long {
        val saleId = insertSale(sale)
        val itemsWithId = items.map { it.copy(saleId = saleId.toInt()) }
        insertSaleItems(itemsWithId)
        for (item in items) {
            updateStock(item.productId, -item.quantity)
        }
        return saleId
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog): Long

    @Transaction
    suspend fun processVoidSaleTransaction(
        saleId: Int,
        username: String,
        reason: String
    ): Boolean {
        val sale = getSaleById(saleId) ?: return false
        if (sale.isVoided) return false
        val items = getSaleItems(saleId)
        for (item in items) {
            updateStock(item.productId, item.quantity)
        }
        updateSale(sale.copy(isVoided = true, voidReason = reason))
        insertAuditLog(
            AuditLog(
                username = username,
                action = "VOID_SALE",
                details = "Sale #$saleId | Amount: ${sale.totalAmount} | Reason: $reason"
            )
        )
        return true
    }

    @Query("SELECT * FROM pos_sale_items WHERE saleId = :saleId")
    suspend fun getSaleItems(saleId: Int): List<POSSaleItem>

    @Query("SELECT * FROM pos_customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<POSCustomer>>

    @Query("SELECT * FROM pos_customers")
    suspend fun getAllCustomersList(): List<POSCustomer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<POSCustomer>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: POSCustomer): Long

    @Delete
    suspend fun deleteCustomer(customer: POSCustomer)

    @Query("SELECT * FROM pos_employees WHERE active = 1 ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<POSEmployee>>

    @Query("SELECT * FROM pos_employees")
    suspend fun getAllEmployeesList(): List<POSEmployee>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(employees: List<POSEmployee>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: POSEmployee): Long

    @Delete
    suspend fun deleteEmployee(employee: POSEmployee)

    @Query("SELECT * FROM pos_suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<POSSupplier>>

    @Query("SELECT * FROM pos_suppliers")
    suspend fun getAllSuppliersList(): List<POSSupplier>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<POSSupplier>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: POSSupplier): Long

    @Delete
    suspend fun deleteSupplier(supplier: POSSupplier)

    @Query("SELECT * FROM pos_stock_adjustments ORDER BY timestamp DESC LIMIT 100")
    fun getAllStockAdjustments(): Flow<List<POSStockAdjustment>>

    @Query("SELECT * FROM pos_stock_adjustments ORDER BY timestamp DESC")
    suspend fun getAllStockAdjustmentsList(): List<POSStockAdjustment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockAdjustments(adjustments: List<POSStockAdjustment>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockAdjustment(adjustment: POSStockAdjustment): Long
}
