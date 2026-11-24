package com.fitlog.app.data

import kotlinx.serialization.Serializable

/** 备份文件结构（data.json） */
@Serializable
data class BackupData(
    val version: Int = 2,
    val exportedAt: Long = 0L,
    val bodyProfiles: List<BodyProfile> = emptyList(),
    val garments: List<Garment> = emptyList(),
    val attrs: List<GarmentAttr> = emptyList(),
    val images: List<GarmentImage> = emptyList(),
    val templates: List<AttrTemplate> = emptyList(),
    val outfits: List<Outfit> = emptyList(),
    val outfitPieces: List<OutfitPiece> = emptyList()
)
