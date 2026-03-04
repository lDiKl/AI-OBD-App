package com.shopai.b2b.data.shop

import com.shopai.b2b.data.local.ShopProfileDao
import com.shopai.b2b.data.local.ShopProfileEntity
import com.shopai.b2b.data.network.ShopApiService
import com.shopai.b2b.data.network.ShopSetupRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShopRepository @Inject constructor(
    private val api: ShopApiService,
    private val dao: ShopProfileDao,
) {

    fun observeProfile(): Flow<ShopProfileEntity?> = dao.observe()

    suspend fun fetchAndCacheProfile(): ShopProfileEntity? {
        return try {
            val dto = api.getShopProfile()
            val entity = ShopProfileEntity(
                name = dto.name,
                address = dto.address,
                phone = dto.phone,
                email = dto.email,
                subscriptionTier = dto.subscriptionTier,
                verified = dto.verified,
            )
            dao.upsert(entity)
            entity
        } catch (e: Exception) {
            dao.get()
        }
    }

    suspend fun setupShop(
        shopName: String,
        address: String,
        phone: String,
        email: String,
    ): ShopProfileEntity {
        val dto = api.setupShop(ShopSetupRequest(shopName, address, phone, email))
        val entity = ShopProfileEntity(
            name = dto.name,
            address = dto.address,
            phone = dto.phone,
            email = dto.email,
            subscriptionTier = dto.subscriptionTier,
            verified = dto.verified,
        )
        dao.upsert(entity)
        return entity
    }

    suspend fun isShopRegistered(): Boolean {
        return try {
            api.getShopProfile()
            true
        } catch (e: Exception) {
            false
        }
    }
}
